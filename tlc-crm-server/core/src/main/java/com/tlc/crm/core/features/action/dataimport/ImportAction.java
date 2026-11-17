package com.tlc.crm.core.features.action.dataimport;

import com.tlc.attachment.io.File;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.model.Record;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.ExecutorEntry;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.RoleService;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.security.permission.annotation.authorizer.LimitAuthorizer;
import com.tlc.file.client.service.AttachmentService;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.features.internal.dataimport.FieldHeader;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;

import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.LimitActionType;
import com.tlc.crm.security.permission.model.RolePermissions;
import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.file.common.queue.AttachmentRemovalPublisher;
import com.tlc.iam.client.UserService;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.core.features.internal.dataimport.async.status.holder.ImportRequestStatusHolder;
import com.tlc.crm.core.features.internal.dataimport.handler.ImportDataHandler;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.settings.SettingsFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.entity.models.PresetConfig;

import com.tlc.crm.core.features.internal.dataimport.async.task.ImportableFileReadTask;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;

import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.core.features.api.dataimport.manager.ImportManager;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.api.dataimport.model.ImportStatistics;
import com.tlc.crm.core.features.api.dataimport.model.converter.ImportConverter;
import com.tlc.crm.core.features.internal.dataimport.async.service.ImportableFileReader;
import com.tlc.crm.core.features.internal.dataimport.ImportFieldService;
import com.tlc.crm.core.features.internal.dataimport.async.registry.ReadTaskRegistry;

import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.status.ImportErrorCodes;

import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.iam.common.models.User;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;

import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.validator.ModelValidator;
import com.tlc.validator.service.provider.ValidatorServiceProvider;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.TreeSet;

import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages {@link ImportInfo} related web actions
 * </p>
 *
 * @author Sundar
 * @version 1.0
 * @see CrmConfigAction
 */
@LimitAuthorizer(types = {LimitActionType.IMPORT_CREATE, LimitActionType.IMPORT_UPDATE}, apiTypes = "import")
@SourceAuthorizer(actions = Action.Type.IMPORT_RECORDS, needCommonApiHandling = true)
@WebAction(path = "/import/mgmt")
public class ImportAction extends AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportAction.class);

    private static final byte DEFAULT_START = 1;
    private static final byte DEFAULT_LIMIT = 20;
    private static final byte PREVIEW_LIMIT = 3;
    private static final int MAX_ROW_LENGTH = 50000;
    private static final String DELIMITERS = "[_\\-\\s]";
    private static final Map<Source, String> SOURCE_HEADER_MAPPING = Map.of(Source.COMPANY, "name");

    private static final String EMPTY_STRING = "";
    private static final StoreType RESTRICTED_TYPE = StoreType.CONTACTS_ONBOARD;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ImportConverter importConverter;
    private final ImportManager importManager;
    private final ImportFieldService importFieldService;
    private final FieldTypeService fieldTypeService;
    private final FieldService fieldService;
    private final ImportRequestStatusHolder importRequestStatusHolder;
    private final ImportDataHandler importDataHandler;
    private final ImportableFileReader importableFileReader;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final SourceSettingsService sourceSettingsService;
    private final FieldSourceService fieldSourceService;
    private final RoleService roleService;
    private final RolePermissionService rolePermissionService;
    private final AttachmentService attachmentService;
    private final AttachmentRemovalPublisher attachmentRemovalPublisher;

    public ImportAction() {
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.importConverter = ImportConverter.getInstance();
        this.importManager = ImportManager.getInstance();
        this.fieldService = FieldService.getSecureInstance();
        this.importFieldService = ImportFieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.importRequestStatusHolder = ImportRequestStatusHolder.getInstance();
        this.importDataHandler = ImportDataHandler.get();
        this.importableFileReader = ImportableFileReader.get();
        this.organizationService = OrganizationService.getInstance();
        this.userService = UserService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.roleService = RoleService.getInstance();
        this.rolePermissionService = RolePermissionService.getInstance();
        this.attachmentService = AttachmentService.getInstance();
        this.attachmentRemovalPublisher = AttachmentRemovalPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Which contains the request payload
     * @param response Represents the response to be sent
     * @throws Exception if any error occurs
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final Long orgId = request.orgId();
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");

        switch (type) {
            case "read" -> readRecordsFromFile(orgId, request, response);
            case "mappingUpdate" -> updateFieldMapping(orgId, data, response);
            case "preview" -> preview(orgId, data, response);
            case "import" -> initImport(request, response);
            case "checkImportStatus" -> checkImportStatus(request, data, response);
            case "list" -> importList(orgId, request, response);
            case "importStatistics" -> importStatistics(orgId, data, response);
            case "fields" -> fetchImportFields(orgId, request, response);
            case "delete" -> delete(orgId, data, response);
            case "stop" -> stop(request, response);
            case "sources" -> response.put(ImportFields.LIST.getName(), getSources(orgId, request.userId()));
        }
    }

    /**
     * Reads the records from uploaded Excel file
     *
     * @param orgId    Represents the organisation ID
     * @param request  Which contains the request payload
     * @param response To be loaded with the data from uploaded file
     */
    private void readRecordsFromFile(final long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ImportFields.DATA.getName());
        final String idKey = ImportFields.ID.getName();
        final ReadTaskRegistry readTaskRegistry = ReadTaskRegistry.get();
        final long attachmentId = data.getLong(idKey);
        final Optional<ImportInfo> optionalImportInfo = importManager.getByAttachmentId(orgId, attachmentId);

        optionalImportInfo.ifPresentOrElse(importInfo -> {
            final long importId = importInfo.id();

            final Consumer<ImportableFileReadTask> readTaskConsumer = (importableFileReadTask -> {

                if (importableFileReadTask.isCompleted()) {

                    if (importableFileReadTask.isSuccess()) {
                        convertRecords(request, response, importableFileReadTask.getImportInfo(), importableFileReadTask.getRecordHolder());
                    } else {
                        response.setErrorCode(ErrorCode.get(ImportErrorCodes.READ_FAILED, "i18n.import.file.read.failed"));
                    }
                    readTaskRegistry.remove(importId);
                } else {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), true);
                }
            });
            final ImportableFileReadTask importableFileReadTask = readTaskRegistry.get(importId);

            if (Objects.isNull(importableFileReadTask)) {
                final ImportableFileReadTask newImportableFileReadTask = importableFileReader.read(importInfo, () -> {
                    final int start = Math.max(DEFAULT_START, data.optInt(ImportFields.START.getName(), DEFAULT_START));
                    final int limit = Math.min(DEFAULT_LIMIT, data.optShort(ImportFields.LIMIT.getName(), DEFAULT_LIMIT));

                    if (start <= MAX_ROW_LENGTH && (limit > 0 && limit <= MAX_ROW_LENGTH)) {
                        final ImportableFileReadTask existingImportableFileReadTask = readTaskRegistry.get(importId);

                        if (Objects.nonNull(existingImportableFileReadTask)) {

                            try {
                                final File file = attachmentService.getFile(organizationService.getIamOrgId(orgId), importInfo.getAttachment().id());
                                final RecordHolder recordHolder = RecordHolder.create();

                                importDataHandler.parse(importInfo, file, start, limit, recordHolder);
                                existingImportableFileReadTask.setRecordHolder(recordHolder);
                            } catch (Exception exception) {
                                LOGGER.error("Import read failed", exception);
                                existingImportableFileReadTask.setStatus(ExecutorEntry.ExecutorStatus.FAILED);
                            }
                        }
                    }
                });
                readTaskConsumer.accept(newImportableFileReadTask);
            } else {
                readTaskConsumer.accept(importableFileReadTask);
            }
        }, () -> sendError(response, null, ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.invalid.id")));
    }

    /**
     * Converts the headers and records stored within the {@link RecordHolder} into JSON format
     *
     * @param request      Which containing the request payload
     * @param response     To be populated with data from the uploaded file
     * @param importInfo   Which contains the uploaded attachment and import information
     * @param recordHolder which contains the collection of records by indices
     */
    private void convertRecords(final CrmRequest request, final CrmResponse response, final ImportInfo importInfo,
                                final RecordHolder recordHolder) {

        if (Objects.nonNull(recordHolder)) {
            final List<String> headers = recordHolder.headers();
            final Long orgId = request.orgId();
            final JsonArray fieldsMap = importInfo.getFieldsMap();

            if (Objects.isNull(fieldsMap)) {
                final JsonArray suggestedMappings = getSuggestedMappings(orgId, headers, response);

                if (Objects.nonNull(suggestedMappings)) {
                    response.put(ImportFields.FIELDS_MAP.getName(), suggestedMappings);
                }
            } else {
                final JsonArray fieldsMapArray = Json.array(fieldsMap.toString());
                final String fieldKey = ImportFields.FIELD.getName();
                final String nameKey = ImportFields.NAME.getName();
                final String sourceKey = ImportFields.SOURCE.getName();
                final String subTypeKey = ImportFields.SUBTYPE.getName();
                final String isMappedKey = ImportFields.IS_MAPPED.getName();
                final String headerIndexKey = ImportFields.HEADER_INDEX.getName();
                final int headerSize = headers.size();
                final Collection<JsonObject> fieldsMapCollection = new TreeSet<>(Comparator.comparingInt(jsonObject -> jsonObject.getInt(headerIndexKey)));
                final Collection<Integer> mappedHeaders = new HashSet<>();

                for (int index = 0; index < fieldsMapArray.size(); index++) {
                    final JsonObject fieldJson = fieldsMapArray.getJsonObject(index);
                    final Optional<FieldInfo> optionalImportField = importFieldService.getField(orgId, Source.get(fieldJson.getString(sourceKey)), fieldJson.getLong(fieldKey));

                    optionalImportField.ifPresent(field -> {

                        if (!fieldJson.optBoolean(isMappedKey)) {
                            fieldJson.put(isMappedKey, true);
                        }

                        if (fieldJson.containsKey(subTypeKey)) {
                            loadFieldSubTypeInfo(fieldTypeService.getSubType(fieldJson.getLong(subTypeKey)), fieldJson, response);
                        } else {
                            fieldJson.put(nameKey, response.resolveI18nPlaceholder(field.i18nName()));
                            mappedHeaders.add(fieldJson.getInt(headerIndexKey));
                        }
                        fieldsMapCollection.add(fieldJson);
                    });
                }

                for (int index = 0; index < headerSize; index++) {

                    if (mappedHeaders.contains(index)) {
                        continue;
                    }
                    fieldsMapCollection.add(buildUnmappedHeaderJson(index));
                }
                response.put(ImportFields.FIELDS_MAP.getName(), buildFieldsMapArray(fieldsMapCollection));
            }
            response.put(ImportFields.HEADERS.getName(), headers);
            response.put(ImportFields.ROWS.getName(), covertFileRowsIntoJsonArray(recordHolder.getRecords()));
            response.put(ImportFields.HAS_MORE.getName(), recordHolder.hasMore());
            response.put(ImportFields.ATTACHMENT.getName(), importConverter.convert(converterConfigBuilder
                    .build(request, false, null), importInfo));
        }
        /*
            In our response, we decided to eliminate the `totalRecords` attribute due to performance concerns associated with calculating the total count.
            This action was taken because including the total records required us to read the entire file, and for large files,
            this process significantly increased the time needed for this operation. Hence, we opted to remove it.
        */
        response.put(ImportFields.ATTACHMENT.getName(), importConverter.convert(converterConfigBuilder.build(request, false, null), importInfo));
    }

    /**
     * Loads the field subtype information into the field header mapping {@link JsonObject}
     *
     * @param fieldSubType The {@link FieldSubType} to be generated
     * @param fieldJson    The {@link JsonObject} to be loaded with the subtype information
     * @param response     Helps resolve the I18N name of the field subtype
     */
    private void loadFieldSubTypeInfo(final FieldSubType fieldSubType, final JsonObject fieldJson, final CrmResponse response) {
        fieldJson.put(ImportFields.SUBTYPE.getName(), fieldSubType.id());
        fieldJson.put(ImportFields.NAME.getName(), response.resolveI18nString(fieldSubType.displayName()));
    }

    /**
     * Converts the {@link Record} into {@link JsonArray}
     *
     * @param fileRowMap Which contains the file rows grouped by the indices
     * @return {@link JsonArray} loaded with the file rows
     */
    private JsonArray covertFileRowsIntoJsonArray(final Map<Integer, Record> fileRowMap) {
        final JsonArray jsonArray = Json.array();

        for (final Record record : fileRowMap.values()) {
            jsonArray.put(record.getColumnValues());
        }
        return jsonArray;
    }

    /**
     * Gets the suggested mappings according to the field names
     *
     * @param orgId    Represents the organisation ID
     * @param headers  Which contains the collection of headers
     * @param response To be loaded with the data from uploaded file
     * @return {@link Map} which contains the suggested mappings grouped by the Entities
     */
    private JsonArray getSuggestedMappings(final Long orgId, final List<String> headers, final CrmResponse response) {

        if (!headers.isEmpty()) {
            final String headerIndexKey = ImportFields.HEADER_INDEX.getName();
            final Collection<JsonObject> suggestions = new TreeSet<>(Comparator.comparingInt(jsonObject -> jsonObject.getInt(headerIndexKey)));
            final Map<Integer, String> headerMap = new HashMap<>();
            final Map<Source, Collection<FieldInfo>> sourceFields = new EnumMap<>(Source.class);
            final int headerSize = headers.size();

            importFieldService.groupAllFieldsBySources(orgId, fieldSourceService.getSources().stream()
                            .filter(FieldSource::isImportable).map(FieldSource::getName).map(Source::get).toList())
                    .forEach(((source, importFields) -> {

                        if (sourceSettingsService.isSourceEnabled(orgId, source.getName())) {
                            sourceFields.put(source, new HashSet<>(importFields));
                        }
                    }));
            loadFieldSuggestion(headers, suggestions, sourceFields, headerMap, response);

            for (int index = 0; index < headerSize; index++) {

                if (!headerMap.containsKey(index)) {
                    continue;
                }
                suggestions.add(buildUnmappedHeaderJson(index));
            }
            return buildFieldsMapArray(suggestions);
        }

        return null;
    }

    /**
     * Builds a {@link JsonObject} containing the unmapped header index and sets 'isMapped' to false
     *
     * @param headerIndex The index of the unmapped header
     * @return A {@link JsonObject} representing the unmapped header index
     */
    private JsonObject buildUnmappedHeaderJson(final int headerIndex) {
        final JsonObject unmappedHeaderJson = Json.object();

        unmappedHeaderJson.put(ImportFields.HEADER_INDEX.getName(), headerIndex);
        unmappedHeaderJson.put(ImportFields.IS_MAPPED.getName(), false);

        return unmappedHeaderJson;
    }

    /**
     * Constructs a {@link JsonArray} comprising the mappings of fields and headers for an import
     *
     * @param fieldsMapCollection Contains the mappings of fields and headers to be assembled
     * @return The resulting {@link JsonArray} encompassing the fields and header mappings
     */
    private JsonArray buildFieldsMapArray(final Collection<JsonObject> fieldsMapCollection) {
        final JsonArray fieldsMapArray = Json.array();

        for (final JsonObject fieldMap : fieldsMapCollection) {
            fieldsMapArray.put(fieldMap);
        }
        return fieldsMapArray;
    }

    /**
     * Loads the subfields information into the response
     *
     * @param storeTypes Which contains the collection of store types
     * @param response   To be loaded with the subfields information
     */
    private void loadSubTypesInfo(final Collection<StoreType> storeTypes, final CrmResponse response) {

        if (!storeTypes.isEmpty()) {
            final JsonObject subFields = Json.object();

            for (final StoreType storeType : storeTypes) {
                final Map<StoreSubType, FieldSubType> groupedSubTypes = switch (storeType) {
                    case ADDRESS -> {
                        final Map<StoreSubType, FieldSubType> addressSubFields = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                        addressSubFields.remove(StoreSubType.LATITUDE);
                        addressSubFields.remove(StoreSubType.LONGITUDE);

                        yield addressSubFields;
                    }
                    default -> fieldTypeService.groupSubtypesByStoreSubType(storeType);
                };
                final JsonArray subTypesArray = Json.array();

                for (final FieldSubType fieldSubType : groupedSubTypes.values()) {
                    final JsonObject subTypeJson = Json.object();

                    subTypeJson.put(ImportFields.ID.getName(), fieldSubType.id());
                    subTypeJson.put(ImportFields.NAME.getName(), response.resolveI18nPlaceholder(fieldSubType.displayName()));

                    subTypesArray.put(subTypeJson);
                }
                subFields.put(storeType.getName(), subTypesArray);
            }
            response.put(FieldKeys.SUB_FIELDS.getName(), subFields);
        }
    }

    /**
     * Loads the field suggestion into the {@link JsonArray}
     *
     * @param headers      Which contains the collection of headers
     * @param suggestions  Which contains the field and header suggestion
     * @param sourceFields Which contains the collection of fields grouped by sources
     * @param headerMap    Which contains the collection of headers those are already reserved for the fields
     * @param response     To be loaded with the data from uploaded file
     */
    private void loadFieldSuggestion(final List<String> headers, final Collection<JsonObject> suggestions,
                                     final Map<Source, Collection<FieldInfo>> sourceFields,
                                     final Map<Integer, String> headerMap, final CrmResponse response) {
        final Map<Source, Map<Integer, String>> sourceHeaders = new HashMap<>();
        final Collection<Source> sources = new ArrayList<>(sourceFields.keySet());
        final Map<Source, Collection<FieldInfo>> unreservedImportFields = new HashMap<>();
        final int headerSize = headers.size();

        for (int columnIndex = 0; columnIndex < headerSize; columnIndex++) {
            final String header = headers.get(columnIndex);
            final String[] headerSegments = header.split(DELIMITERS);
            final int length = headerSegments.length;

            if (length >= 2) {
                final String sourceName = headerSegments[0].toLowerCase();
                final Optional<Source> optionalSource = getSource(sourceName);

                if (optionalSource.isPresent()) {
                    final Source source = optionalSource.get();

                    if (sources.contains(source)) {
                        sourceHeaders.computeIfAbsent(source, columnHeaderMap -> new HashMap<>()).put(columnIndex, header);
                    }
                }
            }

            headerMap.put(columnIndex, header);
        }

        for (final Map.Entry<Source, Map<Integer, String>> sourceHeaderEntry : sourceHeaders.entrySet()) {
            final Source source = sourceHeaderEntry.getKey();
            final Map<Integer, String> columnHeaderMap = sourceHeaderEntry.getValue();
            final Collection<FieldInfo> fields = sourceFields.get(source);

            suggestedMappingFields(fields, headerMap, columnHeaderMap, suggestions, response, false, false);
            unreservedImportFields.put(source, fields);
        }

        if (!headerMap.isEmpty()) {
            final Map<Integer, String> unreservedHeaderMap = new HashMap<>(headerMap);

            for (final Map.Entry<Source, Collection<FieldInfo>> unreservedFieldEntry : unreservedImportFields.entrySet()) {
                suggestedMappingFields(unreservedFieldEntry.getValue(), headerMap, unreservedHeaderMap, suggestions, response, true, true);
            }

            for (final Map.Entry<Source, Collection<FieldInfo>> unreservedFieldEntry : unreservedImportFields.entrySet()) {
                suggestedMappingFields(unreservedFieldEntry.getValue(), headerMap, unreservedHeaderMap,
                        suggestions, response, true, false);
            }

            final Set<Source> unreservedSources = unreservedImportFields.keySet();
            final Map<Source, Collection<FieldInfo>> unmappedFields = sources.stream()
                    .filter(source -> !unreservedSources.contains(source))
                    .collect(Collectors.toMap(source -> source, sourceFields::get));

            for (final Map.Entry<Source, Collection<FieldInfo>> unmappedField : unmappedFields.entrySet()) {
                suggestedMappingFields(unmappedField.getValue(), headerMap, headerMap,
                        suggestions, response, true, false);
            }
        }
    }

    /**
     * <p>
     * Loads suggested mapping fields based on the source and its dependent import fields.
     * </p>
     *
     * @param importFields     Represents the {@link Collection} of import fields
     * @param headerMap        Represents the headers which is already reserved
     * @param suggestions      Represents the {@link Collection} of suggested {@link JsonObject}
     * @param response         Represents the {@link CrmResponse}
     * @param hasPendingField  A flag representing whether the field is pending or not.
     * @param isContentMatches If true, performs an exact match, If false, performs a partial match
     */
    private void suggestedMappingFields(final Collection<FieldInfo> importFields, final Map<Integer, String> headerMap,
                                        final Map<Integer, String> fieldsHeaderMap, final Collection<JsonObject> suggestions,
                                        final CrmResponse response, final boolean hasPendingField, final boolean isContentMatches) {
        final Comparator<FieldInfo> sortByDefaultField = Comparator.comparing(field -> {
            final PresetConfig config = field.presetConfig();

            return Objects.isNull(config) ? Integer.MAX_VALUE : (config.isDefaultField() ? 0 : 1);
        });

        final Comparator<FieldInfo> sortByRequired = Comparator.comparing(field -> {
            final PresetConfig config = field.presetConfig();

            return Objects.isNull(config) ? Integer.MAX_VALUE : (config.required() ? 0 : 1);
        });

        final Collection<FieldInfo> fields = importFields.stream()
                .sorted(sortByDefaultField.thenComparing(sortByRequired)).toList();

        for (final FieldInfo field : fields) {
            final String displayName = trim(response.resolveI18nPlaceholder(field.i18nName().getKey()));

            for (final Map.Entry<Integer, String> headerEntry : fieldsHeaderMap.entrySet()) {
                final Integer headerIndex = headerEntry.getKey();
                final String header = headerEntry.getValue();
                final String[] headerSegments = header.split(DELIMITERS);
                final String name = extractFieldValue(headerSegments, hasPendingField);

                if (!headerMap.containsKey(headerIndex)) {
                    continue;
                }

                if (hasPendingField) {
                    final Optional<Source> optionalSource = getSource(name.toLowerCase());

                    if (optionalSource.isPresent()) {
                        final Source source = optionalSource.get();
                        final String fieldName;

                        if (Source.isPrimarySource(source)) {
                            final PresetConfig config = field.presetConfig();

                            fieldName = (Objects.nonNull(config) && config.isDefaultField()) ? field.aliasName() : response.resolveI18nPlaceholder(field.i18nName().getKey());
                        } else {
                            fieldName = field.aliasName();
                        }

                        if (SOURCE_HEADER_MAPPING.containsKey(source)
                                && fieldName.contentEquals(SOURCE_HEADER_MAPPING.get(source))) {
                            importFields.remove(field);
                            headerMap.remove(headerIndex, header);
                            convertSuggestedFieldToJson(suggestions, field, headerIndex, response);
                            break;
                        }
                    }
                }

                if (hasMatchingContent(displayName, name, isContentMatches) || (hasPendingField && hasMatchingContent(displayName,
                        extractFieldValue(headerSegments, false), isContentMatches))) {
                    importFields.remove(field);
                    headerMap.remove(headerIndex, header);
                    convertSuggestedFieldToJson(suggestions, field, headerIndex, response);
                    break;
                }
            }
        }
    }

    /**
     * <p>
     * Extracts the field value from the header segments.
     * </p>
     *
     * @param headerSegments         Represents the array of header segments.
     * @param hasInitialIndexedField Indicates if the field has an initial indexed position.
     *                               If true, skips the first segment; otherwise, considers all segments.
     * @return The extracted field name.
     */
    private String extractFieldValue(final String[] headerSegments, final boolean hasInitialIndexedField) {
        final int length = headerSegments.length;
        final String value;

        if (length > 1) {
            final StringBuilder builder = new StringBuilder();

            for (int index = (hasInitialIndexedField ? 0 : 1); index < length; index++) {
                builder.append(headerSegments[index]);
            }
            value = trim(builder.toString().trim());
        } else {
            value = headerSegments[0].toLowerCase();
        }

        return value;
    }

    /**
     * <p>
     * Checks if the given field name matches the specified display name based on the provided comparison type.
     * </p>
     *
     * @param displayName     The display name to be checked.
     * @param value           The value to be checked against the display name.
     * @param isContentEquals If true, performs an exact match, If false, performs a partial match
     * @return true, if given both names are matched; otherwise false
     */
    private boolean hasMatchingContent(final String displayName, final String value, final boolean isContentEquals) {
        if (displayName.isEmpty()) {
            return false;
        }

        return isContentEquals ? value.equals(displayName) : value.contains(displayName) || displayName.contains(value);
    }

    /**
     * <p>
     * Gets the {@link Source} based on given source name.
     * </p>
     *
     * @param sourceName Represents the {@link Source}
     * @return {@link Source}
     */
    private Optional<Source> getSource(final String sourceName) {
        try {
            return Optional.ofNullable(Source.get(sourceName.toLowerCase()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * <p>
     * Converts a matched {@link FieldInfo} to a {@link JsonObject} and adds it to the suggestions collection.
     * </p>
     *
     * @param suggestions  Represents the collection of {@link JsonObject} where the matched field information will be added.
     * @param matchedField Represents the matched {@link FieldInfo}.
     * @param index        Represents the index of the header in the original list of headers.
     * @param response     Represents the {@link CrmResponse} for resolving internationalized strings.
     */
    private void convertSuggestedFieldToJson(final Collection<JsonObject> suggestions, final FieldInfo matchedField,
                                             final int index, final CrmResponse response) {
        final JsonObject fieldJson = Json.object();

        fieldJson.put(ImportFields.FIELD.getName(), matchedField.id());
        fieldJson.put(ImportFields.SOURCE.getName(), matchedField.fieldSource().getName());
        fieldJson.put(ImportFields.HEADER_INDEX.getName(), index);

        switch (matchedField.storeType()) {
            case ADDRESS ->
                    loadFieldSubTypeInfo(fieldTypeService.getFieldSubTypeByStoreType(StoreType.ADDRESS, StoreSubType.ADDRESS_LINE1), fieldJson, response);
            case PRICE_TAGS ->
                    loadFieldSubTypeInfo(fieldTypeService.getFieldSubTypeByStoreType(StoreType.PRICE_TAGS, StoreSubType.UNIT_PRICE), fieldJson, response);
            default -> fieldJson.put(ImportFields.NAME.getName(), response.resolveI18nPlaceholder(matchedField.i18nName()));
        }
        fieldJson.put(ImportFields.IS_MAPPED.getName(), true);
        suggestions.add(fieldJson);
    }

    /**
     * Removes the spaces and non-alphabetic letters except digits from given string
     *
     * @param text To be trimmed
     * @return the trimmed String
     */
    private String trim(final String text) {
        return Objects.isNull(text) ? EMPTY_STRING : text.replaceAll("[^a-zA-z0-9]", EMPTY_STRING).toLowerCase();
    }

    /**
     * Updates the mappings of fields and headers along with the relevant import data
     *
     * @param orgId    Represents the organization ID
     * @param data     Contains the mappings of fields and headers
     * @param response The response to be dispatched
     */
    private void updateFieldMapping(final Long orgId, final JsonObject data, final CrmResponse response) {
        final String idKey = ImportFields.ID.getName();
        final String fieldsMapKey = ImportFields.FIELDS_MAP.getName();
        final String headerIndexKey = ImportFields.HEADER_INDEX.getName();
        final String sourceKey = ImportFields.SOURCE.getName();
        final String fieldKey = ImportFields.FIELD.getName();
        final String subTypeKey = ImportFields.SUBTYPE.getName();
        final JsonArray fieldsMapArray = data.getJsonArray(fieldsMapKey);
        final JsonArray validFieldsMap = Json.array();
        final Collection<Integer> mappedIndices = new HashSet<>();
        final Map<Source, Map<Long, Collection<Long>>> mappedSourceFields = new EnumMap<>(Source.class);
        final long importId = data.getLong(idKey);
        final ImportInfo importInfo = importManager.get(orgId, importId);
        final Map<Source, Boolean> sourceStatusMap = new EnumMap<>(Source.class);

        if (ImportInfo.Status.PENDING != importInfo.getStatus()) {
            throw ErrorCode.get(ImportErrorCodes.FIELD_MAPPING_FAILED, "i18n.import.field.mapping.failed");
        }
        final ImportRequestStatusHolder.ImportStatus importStatus = importRequestStatusHolder.getImportStatus(importDataHandler.generateImportRequestId(orgId, importInfo.getUserId(), importId));

        if (Objects.nonNull(importStatus) && importStatus != ImportRequestStatusHolder.ImportStatus.NEW) {
            return;
        }
        for (int index = 0; index < fieldsMapArray.size(); index++) {
            final JsonObject fieldJson = fieldsMapArray.getJsonObject(index);

            if (mappedIndices.add(fieldJson.getInt(headerIndexKey))) {
                final long fieldId = fieldJson.getLong(fieldKey);
                final Source source = Source.get(fieldJson.getString(sourceKey));
                final boolean isSourceEnabled = sourceStatusMap.computeIfAbsent(source,
                        sourceToCheck -> sourceSettingsService.isSourceEnabled(orgId, sourceToCheck.getName()));

                if (!isSourceEnabled) {
                    throw ErrorCode.get(ImportErrorCodes.SOURCE_DISABLED, String.format("i18n.%s.is.disabled", source.getName().toLowerCase()));
                }
                importFieldService.getField(orgId, source, fieldId).ifPresent(field -> {
                    final StoreType storeType = field.storeType();

                    if (StoreType.ADDRESS == storeType || (StoreType.PRICE_TAGS == storeType && fieldJson.containsKey(subTypeKey))) {

                        if (mappedSourceFields.computeIfAbsent(source, newSource -> new HashMap<>())
                                .computeIfAbsent(fieldId, newFieldId -> new HashSet<>()).add(fieldJson.getLong(subTypeKey))) {
                            validFieldsMap.put(fieldJson);
                        }
                    } else {
                        final Map<Long, Collection<Long>> fieldIdMap = mappedSourceFields.computeIfAbsent(source, newSource -> new HashMap<>());

                        if (!fieldIdMap.containsKey(fieldId)) {
                            validFieldsMap.put(fieldJson);
                            fieldIdMap.put(fieldId, null);
                        }
                    }
                });
            }
        }

        if (validFieldsMap.size() > 0) {
            importManager.updateFieldsMap(orgId, importId, validFieldsMap.toString());
        }
        response.put(idKey, importInfo.getAttachment().id());
    }

    /**
     * Loads the response with preview data extracted from the uploaded Excel file
     *
     * @param orgId    Represents the organisation ID
     * @param data     The request data containing necessary information
     * @param response The response to be filled with the preview data
     */
    private void preview(final Long orgId, final JsonObject data, final CrmResponse response) {
        final long importAttachmentId = data.getLong(ImportFields.ID.getName());
        final Optional<ImportInfo> optionalImportInfo = importManager.getByAttachmentId(orgId, importAttachmentId);

        optionalImportInfo.ifPresentOrElse(importInfo -> {
            final RecordHolder recordHolder = RecordHolder.create();
            final File file = attachmentService.getFile(organizationService.getIamOrgId(orgId), importAttachmentId);

            importDataHandler.parse(importInfo, file, (int) DEFAULT_START, (int) PREVIEW_LIMIT, recordHolder);

            if (Objects.nonNull(recordHolder)) {
                final JsonArray previewArray = getPreviewJsonArray(orgId, importInfo.getFieldsMap(), recordHolder, response);

                response.put(ImportFields.ID.getName(), importInfo.id());
                response.put(ImportFields.PREVIEW.getName(), previewArray);
                response.send();
            }
        }, () -> {
            response.setErrorCode(ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found"));
            response.put(ImportFields.ERROR.getName(), response.resolveI18nString("i18n.import.invalid.id"));
            response.send();
        });
    }

    /**
     * Generates a preview JSON array based on the provided organization ID, fields map,
     * record holder, and CRM response.
     *
     * @param orgId        The organization ID, used to filter or process records specific to the organization.
     * @param fieldsMap    The JSON array that maps fields to be included in the preview.
     * @param recordHolder The {@link RecordHolder} that contains the records, which are processed to generate the preview.
     * @param response     The {@link CrmResponse} that contains additional response data used in generating the preview.
     * @return The {@link JsonArray} representing the preview of records, formatted according to the fieldsMap.
     */
    private JsonArray getPreviewJsonArray(final long orgId, final JsonArray fieldsMap, final RecordHolder recordHolder, final CrmResponse response) {
        final JsonArray previewArray = Json.array();

        if (Objects.nonNull(fieldsMap)) {
            final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap = importDataHandler.fetchSourceFieldHeadersMap(orgId, fieldsMap);

            if (!sourceFieldHeadersMap.isEmpty()) {
                final String sourceKey = ImportFields.SOURCE.getName();
                final String headersKey = ImportFields.HEADERS.getName();
                final String rowsKey = ImportFields.ROWS.getName();
                final String headerIndexKey = ImportFields.HEADER_INDEX.getName();
                final Collection<Record> records = recordHolder.getRecords().values();
                final Collection<String> associatedFieldsHeaders = new HashSet<>();
                final Map<Long, String> subTypeNameMap = new HashMap<>();

                loadRelatedFields(orgId, sourceFieldHeadersMap);

                for (final Map.Entry<Source, SourceFieldHeaders> sourceFieldHeaderEntry : sourceFieldHeadersMap.entrySet()) {
                    final String sourceName = sourceFieldHeaderEntry.getKey().getName();

                    if (!sourceSettingsService.isSourceEnabled(orgId, sourceName)) {
                        continue;
                    }
                    final JsonObject sourceJson = Json.object();
                    final JsonArray rows = Json.array();
                    final JsonArray headers = Json.array();
                    final JsonArray sourceHeaders = Json.array();
                    final SourceFieldHeaders fieldHeaders = sourceFieldHeaderEntry.getValue();
                    final BiConsumer<Integer, String> headerIndexMapper = (columnIndex, headerName) -> {
                        final JsonObject jsonObject = Json.object();

                        jsonObject.put(headerIndexKey, columnIndex);
                        jsonObject.put(headersKey, headerName);
                        headers.put(jsonObject);
                    };

                    for (final FieldHeader fieldHeader : fieldHeaders.getSortedFieldHeaders(true)) {
                        final int headerIndex = fieldHeader.getHeaderIndex();

                        if (fieldHeader.isSubFieldHeader()) {
                            final String subTypeName = subTypeNameMap.computeIfAbsent(fieldHeader.getSubFieldId(),
                                    subTypeId -> response.resolveI18nString(fieldTypeService.getSubType(subTypeId).displayName()));

                            headerIndexMapper.accept(headerIndex, subTypeName);
                            sourceHeaders.put(subTypeName);
                        } else {
                            final String fieldName = response.resolveI18nPlaceholder(fieldHeader.getImportField().i18nName());
                            headerIndexMapper.accept(headerIndex, fieldName);
                            sourceHeaders.put(fieldName);
                        }

                        if (fieldHeader.isAssociatedFieldHeader()) {
                            associatedFieldsHeaders.add(response.resolveI18nPlaceholder(fieldHeader.getImportField().i18nName()));
                        }
                    }

                    processRecordsToRows(records, headers, sourceHeaders, associatedFieldsHeaders, rows);
                    sourceJson.put(sourceKey, sourceSettingsService.getSourceSettingsBySource(orgId, sourceName).getAlternateName());
                    sourceJson.put(headersKey, sourceHeaders);
                    sourceJson.put(rowsKey, rows);
                    previewArray.put(sourceJson);
                }
            }
        }

        return previewArray;
    }

    /**
     * Processes a collection of records and organizes their data into structured rows
     * based on the provided headers, source headers, and associated field headers.
     *
     * @param records                 The collection of {@code Record} to process, each representing a row of data.
     * @param headers                 The {@code JsonArray} containing header definitions.
     * @param sourceHeaders           The {@code JsonArray} of source headers representing the order and fields.
     * @param associatedFieldsHeaders The collection of field headers that are associated fields.
     * @param rows                    The {@code JsonArray} that will be populated with processed row arrays derived from each record.
     */
    private void processRecordsToRows(final Collection<Record> records, final JsonArray headers, final JsonArray sourceHeaders,
                                      final Collection<String> associatedFieldsHeaders, final JsonArray rows) {
        final String headersKey = ImportFields.HEADERS.getName();
        final String headerIndexKey = ImportFields.HEADER_INDEX.getName();

        for (final Record row : records) {
            final Map<String, Object> rowsMap = new HashMap<>();
            final Map<String, Object> sourceRows = new HashMap<>();
            final Map<String, Object> associatedSourceRows = new HashMap<>();

            for (int index = 0; index < headers.size(); index++) {
                final JsonObject jsonObject = headers.getJsonObject(index);
                final Object value = row.get(jsonObject.getInt(headerIndexKey));
                final String header = jsonObject.getString(headersKey);

                if (associatedFieldsHeaders.contains(header)) {
                    associatedSourceRows.put(header, value);
                } else if (Objects.nonNull(value)) {
                    sourceRows.put(header, value);
                } else {
                    rowsMap.put(header, null);
                }
            }

            if (!sourceRows.isEmpty()) {

                if (!associatedSourceRows.isEmpty()) {
                    sourceRows.putAll(associatedSourceRows);
                }
                sourceRows.putAll(rowsMap);
                final Collection<Object> rowArray = new ArrayList<>();

                for (int index = 0; index < sourceHeaders.size(); index++) {
                    rowArray.add(sourceRows.getOrDefault(sourceHeaders.getString(index), null));
                }
                rows.put(rowArray);
            }
        }
    }

    /**
     * Imports the data from uploaded file to database
     *
     * @param request  Which contains the request data
     * @param response To be loaded with imported response
     */
    private void initImport(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ImportFields.DATA.getName());
        final Long orgId = request.orgId();
        final Long importId = data.getLong(ImportFields.ID.getName());
        final ImportInfo importInfo = importManager.get(orgId, importId);

        if (Objects.isNull(importInfo.getFieldsMap())) {
            throw ErrorCode.get(ImportErrorCodes.NO_MAPPING_EXIST_BETWEEN_HEADERS_AND_FIELDS, "i18n.import.no.mappings.exist.between.headers.fields");
        }

        switch (importInfo.getStatus()) {

            case FINISHED -> {
                LOGGER.info("Data import: Import is already finished. Org ID: {}, Import ID: {}", orgId, importId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_ALREADY_FINISHED, "i18n.import.already.finished");
            }
            case STOPPED -> {
                LOGGER.info("Data import: Import is stopped. Org ID: {}, Import ID: {}", orgId, importId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_STOPPED, "i18n.import.stopped");
            }
            case REVERTED -> {
                LOGGER.info("Data import: Import is reverted. Org ID: {}, Import ID: {}", orgId, importId);
                throw ErrorCode.get(ImportErrorCodes.IMPORT_ALREADY_REVERTED, "i18n.import.already.reverted");
            }
        }
        final String requestId = importDataHandler.generateImportRequestId(orgId, request.userId(), importId);
        final ImportRequestStatusHolder.ImportStatus importStatus = importRequestStatusHolder.getImportStatus(requestId);

        if (Objects.isNull(importStatus)) {
            final JsonObject importRequestJson = Json.object(data.getBytes());
            final SubscriptionTopic dataImporterSubscriptionTopic = CoreQueueTopics.getDataImporterTopic(QueueModel.Type.KAFKA);
            final KafkaProducerConfig dataImporterProducerConfig = new KafkaProducerConfig(dataImporterSubscriptionTopic);

            importRequestJson.put(ImportFields.LOCALE.getName(), request.locale().toLanguageTag());

            try (final Producer<byte[]> importRequestProducer = QueueServiceProvider.get().createProducer(dataImporterProducerConfig)) {
                importRequestStatusHolder.setImportStatus(requestId, ImportRequestStatusHolder.ImportStatus.NEW);
                importRequestProducer.send(new StreamMessage<>(requestId, dataImporterSubscriptionTopic, importRequestJson.getBytes()));
                LOGGER.info("Data import: Import request published. Import ID: {}, Request ID: {}", importId, requestId);
            }
        } else {

            switch (importStatus) {
                case NEW -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), false);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
                case IMPORTED, STOPPED, IMPORT_FAILED -> {
                    LOGGER.info("Data import: Import is already done. Org ID: {}, Import ID: {}", orgId, importId);
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), false);
                    response.put(ImportFields.IS_DONE.getName(), true);
                }
                case IMPORTING -> {
                    LOGGER.info("Data import: Import is in progress. Request ID: {}", requestId);
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), true);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
            }
        }
        response.put(ImportFields.ID.getName(), importId);
    }

    /**
     * Checks the import status for the given import identifier.
     *
     * @param request    Refers the {@link CrmRequest}.
     * @param jsonObject Refers the request json.
     * @param response   The response to be sent.
     */
    private void checkImportStatus(final CrmRequest request, final JsonObject jsonObject, final CrmResponse response) {
        final long importId = jsonObject.getLong(ImportFields.ID.getName());
        final Long orgId = request.orgId();
        final String importRequestId = importDataHandler.generateImportRequestId(orgId, request.userId(), importId);
        final ImportRequestStatusHolder.ImportStatus importStatus = importRequestStatusHolder.getImportStatus(importRequestId);

        if (Objects.nonNull(importStatus)) {

            switch (importStatus) {
                case IMPORTED -> {
                    final ImportStatistics importStatistics = importManager.getImportStatistics(orgId, importId);

                    response.put(ImportFields.IS_DONE.getName(), true);
                    response.put(ImportFields.INSERTED.getName(), importStatistics.getInsertedCount());
                    response.put(ImportFields.UPDATED.getName(), importStatistics.getUpdatedCount());
                    response.put(ImportFields.MERGED.getName(), importStatistics.getMergedCount());
                    response.put(ImportFields.SKIPPED.getName(), importStatistics.getSkippedCount());
                }
                case IMPORTING -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), true);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
                case NEW -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), false);
                    response.put(ImportFields.IS_DONE.getName(), false);
                }
                case IMPORT_FAILED, STOPPED -> {
                    response.put(ImportFields.IS_IN_PROGRESS.getName(), false);
                    response.put(ImportFields.IS_DONE.getName(), true);
                }
            }
        } else {
            response.setErrorCode(ErrorCode.get(ImportErrorCodes.INVALID_IMPORT_DETAIL_STATUS));
        }
    }

    /**
     * Loads the related fields into the {@link FieldHeader}
     *
     * @param orgId                 Represents the organization ID
     * @param sourceFieldHeadersMap To be loaded with the related entity fields into the {@link FieldHeader}
     */
    private void loadRelatedFields(final long orgId, final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap) {

        for (final Map.Entry<Source, SourceFieldHeaders> sourceFieldHeadersEntry : sourceFieldHeadersMap.entrySet()) {
            final Source source = sourceFieldHeadersEntry.getKey();

            switch (source) {
                case CONTACT -> loadRelatedCompanyFields(orgId, sourceFieldHeadersMap, source);
                case DEAL -> {
                    final Source contactSource = Source.CONTACT;

                    loadRelatedCompanyFields(orgId, sourceFieldHeadersMap, source);

                    if (sourceFieldHeadersMap.containsKey(contactSource)) {
                        final Map<String, FieldInfo> dealFields = fieldService.groupDefaultFieldsByAliasName(Deal.ENTITY_TYPE);
                        final FieldInfo contactField = dealFields.get(DealFields.PRIMARY_CONTACT);
                        final SourceFieldHeaders sourceFieldHeaders = sourceFieldHeadersEntry.getValue();

                        importFieldService.getField(orgId, Source.DEAL, contactField.id()).ifPresent(primaryContactImportField -> {
                            if (!sourceFieldHeaders.contains(primaryContactImportField)) {
                                final FieldInfo contactNameField = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE)
                                        .get(ContactFields.FIRST_NAME);
                                final SourceFieldHeaders contactFieldHeaders = sourceFieldHeadersMap.get(contactSource);

                                importFieldService.getField(orgId, contactSource, contactNameField.id()).ifPresent(contactNameImportField -> {
                                    final Optional<FieldHeader> contactNameHeader = contactFieldHeaders.getFieldHeader(contactNameImportField);

                                    contactNameHeader.ifPresent(fieldHeader -> sourceFieldHeaders.addAssociatedField(primaryContactImportField, fieldHeader));
                                });
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Loads the related company fields into the {@link FieldHeader}
     *
     * @param orgId                 Refers the organization id
     * @param sourceFieldHeadersMap To be loaded with the related entity fields
     * @param source                Represents the source
     */
    private void loadRelatedCompanyFields(final long orgId, final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap, final Source source) {
        final Source companySource = Source.COMPANY;

        if (sourceFieldHeadersMap.containsKey(companySource)) {
            final FieldInfo companyField = fieldService.groupDefaultFieldsByAliasName(source.getName())
                    .get(ContactFields.COMPANY);
            final SourceFieldHeaders sourceFieldHeaders = sourceFieldHeadersMap.get(source);

            importFieldService.getField(orgId, source, companyField.id()).ifPresent(companyImportField -> {

                if (sourceFieldHeadersMap.containsKey(companySource) && !sourceFieldHeaders.contains(companyImportField)) {
                    final SourceFieldHeaders companyFieldHeaderMap = sourceFieldHeadersMap.get(companySource);
                    final FieldInfo companyNameField = fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE)
                            .get(CompanyFields.NAME);

                    importFieldService.getField(orgId, companySource, companyNameField.id()).ifPresent(companyNameImportField -> {
                        final Optional<FieldHeader> companyNameHeader = companyFieldHeaderMap.getFieldHeader(companyNameImportField);

                        companyNameHeader.ifPresent(fieldHeader -> sourceFieldHeaders.addAssociatedField(companyImportField, fieldHeader));
                    });
                }
            });
        }
    }

    /**
     * Loads the {@link ImportInfo} list into the {@link CrmResponse}
     *
     * @param orgId    Represents the organisation ID
     * @param request  Which contains the request payload
     * @param response Which is to be loaded with the statistics
     */
    private void importList(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ImportFields.DATA.getName());
        final int start = Math.max(DEFAULT_START, data.optInt(ImportFields.START.getName(), DEFAULT_START));
        final int limit = Math.min(DEFAULT_LIMIT, data.optInt(ImportFields.LIMIT.getName(), DEFAULT_LIMIT));
        final long totalRecords = importManager.getImportsCount(orgId);

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<ImportInfo> imports = importManager.getImports(orgId, start, limit);

            if (Objects.nonNull(imports) && !imports.isEmpty()) {
                final Collection<Long> attachmentIds = imports.stream().map(importInfo -> importInfo.getAttachment().id()).toList();
                final Map<Long, Attachment> attachments = attachmentService.getAttachments(attachmentIds).stream().collect(Collectors.toMap(Attachment::id, Function.identity()));

                response.convertIntoAsync();

                final Map<Long, Long> userIdMap = crmUserService.getUsers(orgId, imports.stream().map(ImportInfo::getUserId)
                        .collect(Collectors.toSet())).stream().collect(Collectors.toMap(CrmUser::id, CrmUser::getIamId));
                final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                    @Override
                    public void onSuccess(final Collection<User> users) {
                        final JsonArray importArray = Json.array();
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final ImportInfo importInfo : imports) {
                            final Attachment attachment = attachments.get(importInfo.getAttachment().id());

                            if (Objects.nonNull(attachment)) {
                                importInfo.setAttachment(attachment);
                                importArray.put(importConverter.convertJsonForHistory(converterConfigBuilder.build(request, false, null),
                                        importInfo, userMap.get(userIdMap.get(importInfo.getUserId()))));
                            }
                        }
                        response.put(ImportFields.TOTAL_RECORDS.getName(), totalRecords);
                        response.put(ImportFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
                        response.put(ImportFields.LIST.getName(), importArray);
                        response.send();
                    }

                    @Override
                    public void onFailure(final Throwable cause) {
                        sendError(response, null, ErrorCode.get(ImportErrorCodes.USER_RETRIEVAL_FAILED, "i18n.common.user.retrieval.failed", cause));
                    }
                };
                userService.fetchUsers(organizationService.getIamOrgId(orgId), userIdMap.values(), usersFetchCallBack);
            }
        }
        response.put(ImportFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(ImportFields.HAS_MORE.getName(), (start + limit) <= totalRecords);
    }

    /**
     * Loads the import statistics into the {@link CrmResponse}
     *
     * @param orgId    Represents the organisation ID
     * @param data     Which contains the request data
     * @param response Which is to be loaded with the statistics
     */
    private void importStatistics(final Long orgId, final JsonObject data, final CrmResponse response) {
        final ImportStatistics importStatistics = importManager.getImportStatistics(orgId, data.getLong(ImportFields.ID.getName()));

        if (Objects.nonNull(importStatistics)) {
            final JsonArray statisticsList = Json.array();
            final JsonObject mergedCountJson = Json.object();
            final JsonObject addedCountJson = Json.object();
            final JsonObject updatedCountJson = Json.object();
            final JsonObject skippedCountJson = Json.object();
            final String nameKey = ImportFields.NAME.getName();
            final String countKey = ImportFields.COUNT.getName();

            mergedCountJson.put(nameKey, response.resolveI18nString("i18n.import.statistics.merged"));
            mergedCountJson.put(countKey, importStatistics.getMergedCount());

            addedCountJson.put(nameKey, response.resolveI18nString("i18n.import.statistics.added"));
            addedCountJson.put(countKey, importStatistics.getInsertedCount());

            updatedCountJson.put(nameKey, response.resolveI18nString("i18n.import.statistics.updated"));
            updatedCountJson.put(countKey, importStatistics.getUpdatedCount());

            skippedCountJson.put(nameKey, response.resolveI18nString("i18n.import.statistics.skipped"));
            skippedCountJson.put(countKey, importStatistics.getSkippedCount());

            statisticsList.put(mergedCountJson);
            statisticsList.put(addedCountJson);
            statisticsList.put(updatedCountJson);
            statisticsList.put(skippedCountJson);

            response.put(ImportFields.LIST.getName(), statisticsList);
        }
    }

    /**
     * Retrieve the required data fields for importing
     *
     * @param orgId    Represents the organisation ID
     * @param request  Which contains the request payload
     * @param response Represents the response to be sent
     */
    private void fetchImportFields(final Long orgId, final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ImportFields.DATA.getName());
        final String sourceName = data.getString(ImportFields.SOURCE.getName());
        final Source source = Source.get(sourceName);
        final Collection<StoreType> storeTypes = new HashSet<>();
        final JsonArray jsonArray = Json.array();

        switch (source) {
            case NOTE, ACTIVITY -> buildFieldsForUnregisteredSource(orgId, source, storeTypes, jsonArray, response);
            default -> {
                final Collection<StoreType> nonRequiredSubfieldTypes = Set.of(StoreType.EMAIL, StoreType.EMAILS,
                        StoreType.PHONE, StoreType.PHONES, StoreType.DEAL_VALUE, StoreType.MONETARY,
                        StoreType.DATE_RANGE, StoreType.TIME_RANGE, StoreType.DATE_TIME_RANGE);
                final String idKey = ImportFields.ID.getName();

                for (final Map.Entry<Integer, Collection<FieldInfo>> fieldsEntry : importFieldService.groupAllFieldsByType(orgId, sourceName).entrySet()) {
                    final StoreType storeType = StoreType.valueOf(fieldsEntry.getKey());

                    if (RESTRICTED_TYPE == storeType || StoreType.AUTO_NUMBER == storeType) {
                        continue;
                    }

                    if (!nonRequiredSubfieldTypes.contains(storeType) && storeType.hasSubtypes()) {
                        storeTypes.add(storeType);
                    }

                    for (final FieldInfo field : fieldsEntry.getValue()) {
                        jsonArray.put(convertEntityField(field, response));
                    }
                }
                response.put(ImportFields.IS_REGISTERED_SOURCE.getName(), true);
            }
        }

        if (jsonArray.size() > 0) {
            response.put(FieldKeys.FIELDS.getName(), jsonArray);
        }
        loadSubTypesInfo(storeTypes, response);
    }

    /**
     * <p>
     * Builds the fields for a unregistered source in a json array
     * </p>
     *
     * @param orgId      Represents the organisation ID
     * @param source     Represents the source
     * @param jsonArray  Represents the json array to build the fields
     * @param storeTypes Represents the collection of store types.
     * @param response   Represents the response to be sent
     */
    private void buildFieldsForUnregisteredSource(final long orgId, final Source source, final Collection<StoreType> storeTypes,
                                                  final JsonArray jsonArray, final CrmResponse response) {
        for (final Map.Entry<StoreType, List<FieldInfo>> fieldsEntry : importFieldService.groupFieldsByStoreType(orgId, source).entrySet()) {
            final StoreType storeType = fieldsEntry.getKey();

            if (RESTRICTED_TYPE == storeType) {
                continue;
            }

            if (storeType.hasSubtypes()) {
                storeTypes.add(storeType);
            }

            for (final FieldInfo field : fieldsEntry.getValue()) {
                jsonArray.put(convertImportField(field, response));
            }
        }
    }

    /**
     * Converts the {@link FieldInfo} into {@link JsonObject}
     *
     * @param field    Represent the {@link FieldInfo}
     * @param response Represents the response to be sent. It is used here to resolve the field names according to the locale
     * @return {@link JsonObject} that contains the field information
     */
    private JsonObject convertImportField(final FieldInfo field, final CrmResponse response) {
        final JsonObject fieldJson = Json.object();

        fieldJson.put(ImportFields.ID.getName(), field.id());
        fieldJson.put(ImportFields.TYPE.getName(), field.storeType().getName());
        fieldJson.put(ImportFields.NAME.getName(), response.resolveI18nPlaceholder(field.i18nName()));

        return fieldJson;
    }

    /**
     * Converts the entity related field
     *
     * @param field    Represent the field
     * @param response Represents the response to be sent. It is used here to resolve the field names according to the locale
     * @return {@link JsonObject} that contains the field information
     */
    private JsonObject convertEntityField(final FieldInfo field, final CrmResponse response) {
        final JsonObject fieldJson = Json.object();
        final StoreType storeType = field.storeType();

        fieldJson.put(ImportFields.ID.getName(), field.id());
        fieldJson.put(ImportFields.TYPE.getName(), storeType.getName());
        fieldJson.put(ImportFields.NAME.getName(), response.resolveI18nPlaceholder(field.i18nName()));

        if (StoreType.PRICE_TAGS == storeType) {
            fieldJson.put(ImportFields.IS_ARRAY.getName(), true);
        }

        if (field.isPersistent()) {
            fieldJson.put(ImportFields.IS_CUSTOM_FIELD.getName(), true);
        }
        return fieldJson;
    }

    /**
     * <p>
     * Deletes the import details for the given data
     * </p>
     *
     * @param orgId    Represents an org id
     * @param data     Represents the request json
     * @param response Holds the response
     */
    private void delete(final Long orgId, final JsonObject data, final CrmResponse response) {
        final Long importId = data.getLong(ImportFields.ID.getName());

        if (importManager.exists(orgId, importId)) {
            final ImportInfo importInfo = importManager.get(orgId, importId);

            switch (importInfo.getStatus()) {
                case PENDING, IN_PROGRESS -> {
                    importManager.delete(importInfo);
                    attachmentRemovalPublisher.publish(String.valueOf(importId), List.of(importInfo.getAttachment().id()));
                }
                case FINISHED ->
                        response.setErrorCode(ErrorCode.get(ImportErrorCodes.IMPORT_ALREADY_FINISHED, "i18n.import.already.finished"));
                case REVERTED ->
                        response.setErrorCode(ErrorCode.get(ImportErrorCodes.IMPORT_ALREADY_REVERTED, "i18n.import.already.reverted"));
            }
        } else {
            response.setErrorCode(ErrorCode.get(ImportErrorCodes.ENTITY_NOT_FOUND, "i18n.import.entity.not.found"));
        }
    }

    /**
     * <p>
     * Stops the import process for the given request
     * </p>
     *
     * @param request  Which contains the request data
     * @param response Holds the response
     */
    private void stop(final CrmRequest request, final CrmResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(ImportFields.DATA.getName());
        final long importId = data.getLong(ImportFields.ID.getName());
        final long orgId = request.orgId();
        final String requestId = importDataHandler.generateImportRequestId(orgId, request.userId(), importId);

        importRequestStatusHolder.setImportStatus(requestId, ImportRequestStatusHolder.ImportStatus.STOPPED);
        importManager.updateStatus(orgId, importId, ImportInfo.Status.STOPPED.getType());

        response.put(ImportFields.MESSAGE.getName(), response.resolveI18nString("i18n.import.stopped"));
    }

    /**
     * <p>
     * Retrieves all the import source settings for a given organization ID
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link JsonArray} containing import sources
     */
    private JsonArray getSources(final Long orgId, final Long userId) {
        final CrmRole crmRole = roleService.getRoleByUser(orgId, userId);
        final RolePermissions rolePermissions = rolePermissionService.getRolePermissions(orgId, crmRole.id());
        final JsonArray importSources = Json.array();
        final Collection<SourceSettings> models = sourceSettingsService.getImportSourceSettings(orgId);
        final Category.Type sourcesCategory = Category.Type.SOURCES;
        final Action.Type createAction = Action.Type.CREATE;
        final Action.Type editAction = Action.Type.EDIT;
        final Map<Source, Map<Action.Type, Boolean>> rolePermissionsMap = new EnumMap<>(Source.class);

        rolePermissions.getPermissions().stream().filter(rolePermission -> rolePermission.getCategory().getType().equals(sourcesCategory))
                .filter(rolePermission -> {
                    final Action.Type actionType = rolePermission.getAction().getType();

                    return actionType.equals(createAction) || actionType.equals(editAction);
                }).forEach(rolePermission -> rolePermissionsMap.computeIfAbsent(Source.get(rolePermission
                        .getSubCategory().getType().getAliasName()), permissions -> new EnumMap<>(Action.Type.class))
                        .computeIfAbsent(rolePermission.getAction().getType(), access -> rolePermission.isGranted())
                );

        for (final SourceSettings sourceSettings : models) {
            final String sourceName = fieldSourceService.getSource(sourceSettings.getSourceId()).getName();
            final Source source = Source.get(sourceName);
            final boolean isSourceEnabled = sourceSettings.isEnabled();
            final boolean isImportSourceEnabled;

            if (rolePermissionsMap.containsKey(source)) {
                final Map<Action.Type, Boolean> actionPermissionMap = rolePermissionsMap.get(source);
                final boolean isPermissionEnabled = actionPermissionMap.get(createAction) || actionPermissionMap.get(editAction);
                isImportSourceEnabled = isPermissionEnabled && isSourceEnabled;
            } else {
                isImportSourceEnabled = isSourceEnabled;
            }
            importSources.put(convertSourceSettingsToJson(sourceSettings, sourceName, isImportSourceEnabled));
        }
        return importSources;
    }

    /**
     * <p>
     * Converts the {@link SourceSettings} into {@link JsonObject}
     * </p>
     *
     * @param sourceSettings        Represents the {@link SourceSettings}
     * @param sourceName            Represents the source name
     * @param isImportSourceEnabled Represents whether the import source is enabled or not
     * @return The {@link JsonObject}
     */
    private JsonObject convertSourceSettingsToJson(final SourceSettings sourceSettings, final String sourceName,
                                                   final boolean isImportSourceEnabled) {
        final JsonObject sourceSettingsJson = Json.object();

        sourceSettingsJson.put(SettingsFields.SOURCE_ID.getName(), sourceSettings.getSourceId());
        sourceSettingsJson.put(SettingsFields.ALTERNATE_NAME.getName(), sourceSettings.getAlternateName());
        sourceSettingsJson.put(SettingsFields.SOURCE_NAME.getName(), sourceName);
        sourceSettingsJson.put(SettingsFields.IS_ENABLED.getName(), isImportSourceEnabled);

        return sourceSettingsJson;
    }
}
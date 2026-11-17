package com.tlc.crm.core.features.internal.dataimport.handler.impl;

import com.tlc.attachment.io.Extension;
import com.tlc.attachment.io.File;
import com.tlc.attachment.model.RecordHolder;
import com.tlc.attachment.reader.DataReader;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.JavaUtil;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.deal.manager.DealManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.features.api.dataimport.model.SourceRecord;
import com.tlc.crm.core.features.internal.dataimport.ImportFieldService;
import com.tlc.crm.core.features.internal.dataimport.holder.impl.ImportableRecordHolder;
import com.tlc.crm.entity.validator.EntityViolationsCache;
import com.tlc.crm.core.features.internal.dataimport.loader.DataHolder;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.crm.core.features.internal.dataimport.FieldHeader;
import com.tlc.crm.core.features.internal.dataimport.MergeConfig;
import com.tlc.crm.core.features.internal.dataimport.FieldSourcePair;
import com.tlc.crm.core.features.internal.dataimport.SourceFieldHeaders;
import com.tlc.crm.core.features.internal.dataimport.container.SourceModelContainer;
import com.tlc.crm.core.features.internal.dataimport.container.SourceRecordContainer;
import com.tlc.crm.core.features.internal.dataimport.handler.ImportDataHandler;
import com.tlc.crm.core.features.internal.dataimport.tracker.ErrorTracker;
import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.TlcModel;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import jakarta.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Handles the required services for the import related actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ImportDataHandlerImpl implements ImportDataHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportDataHandlerImpl.class);

    private final FieldService fieldService;
    private final ImportFieldService importFieldService;
    private final ModelValidator modelValidator;
    private final EntityViolationsCache entityViolationsCache;
    private final ContactManager contactManager;
    private final CompanyManager companyManager;
    private final DealManager dealManager;
    private final ProductManager productManager;
    private final int modelDataFetchBatchSize;
    private final int modelIdFetchBatchSize;

    private ImportDataHandlerImpl() {
        this.fieldService = FieldService.getInstance();
        this.importFieldService = ImportFieldService.getInstance();
        this.modelValidator = ValidatorServiceProvider.get().modelValidator();
        this.entityViolationsCache = EntityViolationsCache.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
        this.dealManager = DealManager.getInstance();
        this.productManager = ProductManager.getInstance();
        this.modelDataFetchBatchSize = Integer.parseInt(ConfHolder.getInstance().getProperties("crm-common")
                .optConfig("import.data.fetch.batch.size", "50"));
        this.modelIdFetchBatchSize = Integer.parseInt(ConfHolder.getInstance().getProperties("crm-common")
                .optConfig("import.data.fetch.batch.size", "1000"));
    }

    private static class Instance {
        private static final ImportDataHandlerImpl INSTANCE = new ImportDataHandlerImpl();
    }

    /**
     * Gets the instance of {@link ImportDataHandlerImpl}
     *
     * @return {@link ImportDataHandlerImpl}
     */
    public static ImportDataHandlerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo Which contains the uploaded attachment and import information
     * @param start      Refers to where the reading of the file should be started
     * @param limit      Refers to where the reading of the file should be stopped
     */
    @Override
    public void parse(final ImportInfo importInfo, final File file, final Integer start, final Integer limit, final RecordHolder recordHolder) {
        final Attachment attachment = importInfo.getAttachment();
        final long importId = importInfo.id();
        LOGGER.info("Data import: Uploaded attachment info {}. Import ID: {}", attachment, importId);

        if (Objects.nonNull(attachment)) {
            LOGGER.info("Data import: File information obtained to initiate the read operation for the import process. Import ID: {}, File info: {}", importId, file);

            if (Objects.nonNull(file)) {
                final DataReader dataReader = DataReader.get(Extension.get(file.extension()));

                if (Objects.isNull(start) && Objects.isNull(limit)) {
                    dataReader.read(file, recordHolder);
                } else {
                    dataReader.read(file, start, limit, recordHolder);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization ID
     * @param fieldsArray An array containing information about the mapping between fields and header indices
     * @return A {@link Map} that shows the relationship between fields and header indices, grouped by their sources
     */
    @Override
    public Map<Source, SourceFieldHeaders> fetchSourceFieldHeadersMap(final Long orgId, final JsonArray fieldsArray) {
        final String headerIndexKey = ImportFields.HEADER_INDEX.getName();
        final String sourceKey = ImportFields.SOURCE.getName();
        final String fieldKey = ImportFields.FIELD.getName();
        final String subTypeKey = ImportFields.SUBTYPE.getName();
        final Collection<Integer> mappedIndices = new HashSet<>();
        final Map<StoreType, Collection<Long>> subTypesByStoreType = new EnumMap<>(StoreType.class);
        final FieldTypeService fieldTypeService = FieldTypeService.getInstance();
        final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap = new EnumMap<>(Source.class);

        for (int index = 0; index < fieldsArray.size(); index++) {
            final JsonObject fieldMapJson = fieldsArray.getJsonObject(index);
            final Source source = Source.get(fieldMapJson.getString(sourceKey));

            importFieldService.getField(orgId, source, fieldMapJson.getLong(fieldKey)).ifPresent(importField -> {
                final int headerIndex = fieldMapJson.getInt(headerIndexKey);

                if (mappedIndices.add(headerIndex)) {

                    if (fieldMapJson.containsKey(subTypeKey)) {
                        final Collection<Long> fieldSubTypeIds = subTypesByStoreType.computeIfAbsent(importField.storeType(), storeType -> {
                            final Collection<FieldSubType> fieldSubTypes = fieldTypeService.getSubTypesByStoreType(storeType);

                            return fieldSubTypes.stream().map(FieldSubType::id).collect(Collectors.toSet());
                        });
                        final long subTypeId = fieldMapJson.getLong(subTypeKey);

                        if (fieldSubTypeIds.contains(subTypeId)) {
                            sourceFieldHeadersMap.computeIfAbsent(source, SourceFieldHeaders::new)
                                    .add(new FieldHeader(importField, headerIndex, subTypeId));
                        }
                    } else {
                        sourceFieldHeadersMap.computeIfAbsent(source, SourceFieldHeaders::new)
                                .add(new FieldHeader(importField, headerIndex));
                    }
                }
            });
        }
        return sourceFieldHeadersMap;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId
     * @param mergeRequired         A flag that represents the additional merge configurations are needed or not
     * @param sourceFieldHeadersMap Which holds information about the mapping between fields and headers, organized by the source
     * @return {@link MergeConfig}
     */
    @Override
    public MergeConfig getDefaultMergeConfig(final Long orgId, final boolean mergeRequired, final Map<Source, SourceFieldHeaders> sourceFieldHeadersMap) {
        final MergeConfig mergeConfig = new MergeConfig();
        final Source companySource = Source.COMPANY;
        final Source contactSource = Source.CONTACT;
        final String idKey = ImportFields.ID.getName();

        final BiConsumer<Collection<FieldInfo>, FieldInfo> mergeConfigFieldAdder = (mappedFields, importField) -> {

            if (mappedFields.contains(importField)) {
                mergeConfig.add(importField);
            }
        };

        if (sourceFieldHeadersMap.containsKey(companySource)) {
            final SourceFieldHeaders companyFieldHeaders = sourceFieldHeadersMap.get(companySource);

            if (Objects.nonNull(companyFieldHeaders) && mergeRequired) {
                final Collection<FieldInfo> mappedCompanyFields = companyFieldHeaders.getFields();
                final String companyNameKey = CompanyFields.NAME;
                final Optional<FieldInfo> optionalCompanyNameImportField = importFieldService.getField(orgId, companySource,
                        fieldService.groupDefaultFieldsByAliasName(Company.ENTITY_TYPE, companyNameKey).get(companyNameKey).id());

                optionalCompanyNameImportField.ifPresent(companyNameImportField -> mergeConfigFieldAdder.accept(mappedCompanyFields, companyNameImportField));
                mergeConfigFieldAdder.accept(mappedCompanyFields, importFieldService.groupDefaultFieldsByAliasName(orgId, companySource).get(idKey));
            }
        }

        if (sourceFieldHeadersMap.containsKey(contactSource)) {
            final SourceFieldHeaders contactFieldHeaders = sourceFieldHeadersMap.get(contactSource);

            if (Objects.nonNull(contactFieldHeaders) && mergeRequired) {
                final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE,
                        ContactFields.EMAILS, ContactFields.PHONES);
                final Collection<FieldInfo> mappedContactFields = contactFieldHeaders.getFields();

                mergeConfigFieldAdder.accept(mappedContactFields, importFieldService.groupDefaultFieldsByAliasName(orgId, contactSource).get(idKey));

                for (final FieldInfo field : contactFields.values()) {
                    importFieldService.getField(orgId, contactSource, field.id())
                            .ifPresent(importField -> mergeConfigFieldAdder.accept(mappedContactFields, importField));
                }
            }
        }
        return mergeConfig;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation ID
     * @param userId Represents the user ID
     * @param importId Represents the import ID
     * @return the generated import request ID
     */
    @Override
    public String generateImportRequestId(final long orgId, final long userId, final long importId) {
        return Base64.getEncoder().encodeToString(String.format("%s:%s:%s", importId, orgId, userId).getBytes());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization ID
     * @param mappedSources         Which holds information about the mapping between fields and headers, organized by the source
     * @param sourceRecordContainer Which is to be loaded with the related source fields
     */
    @Override
    public void loadRelatedFields(final long orgId, final Collection<Source> mappedSources, final SourceRecordContainer sourceRecordContainer) {
        mappedSources.forEach(source -> {

            switch (source) {
                case CONTACT -> loadRelatedFields(sourceRecordContainer, mappedSources,
                        importFieldService.groupDefaultFieldsByAliasName(orgId, source), ContactFields.COMPANY, Source.COMPANY);
                case DEAL -> {
                    final Map<String, FieldInfo> dealFields = importFieldService.groupDefaultFieldsByAliasName(orgId, Source.DEAL);

                    loadRelatedFields(sourceRecordContainer, mappedSources, dealFields, DealFields.COMPANY,
                            Source.COMPANY);
                    loadRelatedFields(sourceRecordContainer, mappedSources, dealFields, DealFields.PRIMARY_CONTACT,
                            Source.CONTACT);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization ID
     * @param sourceRecordContainer Which is to be loaded with the associated source fields
     */
    @Override
    public void injectDistinctFields(final Long orgId, final SourceRecordContainer sourceRecordContainer) {
        final Source contactSource = Source.CONTACT;
        final Source companySource = Source.COMPANY;
        final Source productSource = Source.PRODUCT;
        final Source dealSource = Source.DEAL;
        final Source activitySource = Source.ACTIVITY;
        final Map<String, FieldInfo> fields = importFieldService.groupDefaultFieldsByAliasName(orgId, activitySource);

        sourceRecordContainer.loadDistinctFields(companySource, getDistinctImportFields(orgId, companySource));
        sourceRecordContainer.loadDistinctFields(contactSource,
                getDistinctImportFields(orgId, contactSource, ContactFields.LIFECYCLE_STAGE, ContactFields.LIFECYCLE_STATUS));
        sourceRecordContainer.loadDistinctFields(dealSource,
                getDistinctImportFields(orgId, dealSource, DealFields.PIPELINE, DealFields.PIPELINE_STAGE,
                        DealFields.STATUS));
        sourceRecordContainer.loadDistinctFields(productSource, getDistinctImportFields(orgId, productSource));
        sourceRecordContainer.loadDistinctFields(activitySource, List.of(fields.get(ActivityFields.TYPE_ID.getName()),
                fields.get(ActivityFields.PRIORITY.getName()), fields.get(ActivityFields.IS_DONE.getName()),
                fields.get(ActivityFields.REMINDER.getName()), fields.get(ActivityFields.AVAILABILITY.getName())));
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceModelContainer is a repository that encompasses all the model values extracted from the {@link SourceRecordContainer} Which contains the {@link Collection} of source models grouped by each row index
     * @param errorTracker         Tracks the errors that arise while validating the source models
     */
    @Override
    public void validateModels(final SourceModelContainer sourceModelContainer, final ErrorTracker errorTracker) {
        final Map<Integer, Map<Source, TlcModel>> dataMap = sourceModelContainer.getDataMap();
        final long startTime = System.currentTimeMillis();
        final long importId = errorTracker.getImportId();
        final long orgId = sourceModelContainer.getOrgId();
        final BaseCrmUser user = LoggedInUserInfo.getUser();
        final Thread mainThread = Thread.currentThread();

        try {
            dataMap.entrySet().parallelStream().forEach(dataEntry -> {
                entityViolationsCache.setEnabled(true);
                final Integer index = dataEntry.getKey();

                try {
                    if (!Objects.equals(mainThread, Thread.currentThread())) {
                        SecurityContext.load(user);
                    }
                    final Map<Source, TlcModel> sourceModelMap = dataEntry.getValue();

                    for (final Map.Entry<Source, TlcModel> sourceModelEntry : sourceModelMap.entrySet()) {
                        final TlcModel model = sourceModelEntry.getValue();
                        final Source source = sourceModelEntry.getKey();
                        final long modelValidationStartTime = System.currentTimeMillis();

                        LOGGER.debug("Data import: Validating {} model, index:{}, import ID: {}", source.getName(), index, importId);
                        try {
                            final Set<ConstraintViolation<TlcModel>> violations = modelValidator.validate(model,
                                    Objects.isNull(model.id()) ? Group.Create.class : Group.Update.class);

                            if (Objects.nonNull(violations) && !violations.isEmpty()) {
                                final Collection<String> errors = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());

                                errorTracker.track(index, Collections.unmodifiableCollection(errors));
                            }
                            LOGGER.debug("Data import: {} model validation completed in {} ms, index:{}, import ID: {}", source.getName(), System.currentTimeMillis() - modelValidationStartTime, index, importId);
                        } catch (Throwable throwable) {
                            final String sourceName = source.getName();

                            errorTracker.track(index, String.format("i18n.%s.validation.failed", sourceName.toLowerCase()));
                            LoggerUtil.logError(LOGGER, throwable, "Data import: {} validation failed, Index: {}, Import ID: {}", sourceName, index, importId);
                        }
                    }
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Data import: validation failed for the row {}, Org ID: {}, Import ID: {}, message: {}", index, orgId, importId, throwable.getMessage(), throwable);
                } finally {

                    if (!Objects.equals(mainThread, Thread.currentThread())) {
                        SecurityContext.clear();
                        entityViolationsCache.setEnabled(false);
                    }
                }
            });
        } finally {
            entityViolationsCache.setEnabled(false);
            entityViolationsCache.clear();
        }
        LOGGER.info("Data import: Model validation took {} milliseconds after the conversion. Import ID: {}", System.currentTimeMillis() - startTime, importId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                 Represents the organization ID
     * @param sources               The collection of {@link Source}
     * @param recordHolder          Represents the {@link RecordHolder}
     * @param sourceRecordContainer Represents the {@link SourceRecordContainer}
     */
    @Override
    public void loadRecordsIntoContainer(final Long orgId, final Collection<Source> sources, final RecordHolder recordHolder,
                                         final SourceRecordContainer sourceRecordContainer) {
        if (recordHolder instanceof final ImportableRecordHolder importableRecordHolder) {
            final Map<FieldInfo, Map<Object, Collection<Integer>>> indexedData = importableRecordHolder.getIndexedData();

            for (final Source source : sources) {

                switch (source) {
                    case CONTACT -> {
                        final Map<String, FieldInfo> contactFields = importFieldService.groupDefaultFieldsByAliasName(orgId, source);
                        final Map<Object, Collection<Integer>> indicesByEmail = indexedData.get(contactFields.get(ContactFields.EMAILS));
                        final Map<Object, Collection<Integer>> indicesByPhone = indexedData.get(contactFields.get(ContactFields.PHONES));

                        sourceRecordContainer.fetchSourceModelIndicesMap(source).putAll(groupContactIdsByEmailPhone(orgId, indicesByEmail, indicesByPhone));
                    }
                    case COMPANY -> {
                        final Map<String, FieldInfo> companyFields = importFieldService.groupDefaultFieldsByAliasName(orgId, source);
                        final Map<Object, Collection<Integer>> sourceRecordsByName = indexedData.get(companyFields.get(CompanyFields.NAME));

                        sourceRecordContainer.fetchSourceModelIndicesMap(source).putAll(groupCompanyIdsByName(orgId, sourceRecordsByName));
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sources    The collection of {@link Source}
     * @param dataHolder Represents the {@link DataHolder}
     */
    @Override
    public void fetchAndStoreSourceDataContainers(final Collection<Source> sources, final DataHolder dataHolder) {
        final Long orgId = dataHolder.orgId();
        final SourceRecordContainer sourceRecordContainer = dataHolder.sourceRecordContainer();
        final SourceModelContainer sourceModelContainer = dataHolder.sourceModelContainer();

        for (final Source source : sources) {
            final Map<String, FieldInfo> entityFields = importFieldService.groupDefaultFieldsByAliasName(orgId, source);
            final Map<Object, SourceRecord> entityIdRecordMap = sourceRecordContainer.indexData(source)
                    .computeIfAbsent(entityFields.get(ImportFields.ID.getName()), idMap -> new HashMap<>());
            final Collection<Long> entityIdCollection = new HashSet<>(entityIdRecordMap.keySet().stream()
                    .map(Long.class::cast).collect(Collectors.toUnmodifiableSet()));

            if (!entityIdCollection.isEmpty()) {

                switch (source) {
                    case CONTACT -> fetchAndStoreModelDataContainers(orgId, source, sourceModelContainer, entityIdCollection, contactManager::fetchContactContainer);
                    case COMPANY -> fetchAndStoreModelDataContainers(orgId, source, sourceModelContainer, entityIdCollection, companyManager::fetchCompanyContainer);
                    case DEAL -> fetchAndStoreModelDataContainers(orgId, source, sourceModelContainer, entityIdCollection, dealManager::fetchDealContainer);
                    case PRODUCT -> fetchAndStoreModelDataContainers(orgId, source, sourceModelContainer, entityIdCollection, productManager::fetchDataContainer);
                }
            }
        }
    }

    /**
     * Retrieves the importable distinct fields
     *
     * @param orgId      Represents the organization ID
     * @param source     Represents the source
     * @param fieldNames Which contains the array of distinct field names
     * @return {@link Collection} of importable distinct fields
     */
    private Collection<FieldInfo> getDistinctImportFields(final Long orgId, final Source source, final String... fieldNames) {
        final Collection<FieldInfo> distinctImportFields = new HashSet<>();

        for (final FieldInfo fieldInfo : fieldService.groupDefaultFieldsByAliasName(source.getName(), fieldNames).values()) {
            importFieldService.getField(orgId, source, fieldInfo.id()).ifPresent(distinctImportFields::add);
        }
        return distinctImportFields;
    }

    /**
     * <p>
     * Groups the indices by existing company ids retrieved using their names
     * </p>
     *
     * @param orgId         Represents the organization ID
     * @param indicesByName The map containing company name as key and its index as value
     * @return The map containing company id as key and its respective indices as value
     */
    private Map<Long, Collection<Integer>> groupCompanyIdsByName(final Long orgId, final Map<Object, Collection<Integer>> indicesByName) {

        if (Objects.nonNull(indicesByName) && !indicesByName.isEmpty()) {
            final Collection<String> companyNames = indicesByName.keySet().stream().filter(String.class::isInstance).map(String.class::cast).toList();
            final Map<Long, Collection<Integer>> companyIdsGroupedByName = new HashMap<>();
            final Collection<Collection<String>> batchedNamesCollection = JavaUtil.splitCollection(companyNames, modelIdFetchBatchSize);
            final Collection<Long> emptyList = Collections.emptyList();

            for (final Collection<String> batchedNames : batchedNamesCollection) {
                final Map<String, Long> nameIdMap = companyManager.getCompanyIdsByName(orgId, batchedNames, emptyList);

                for (final Map.Entry<String, Long> companyEntry : nameIdMap.entrySet()) {
                    final String companyName = companyEntry.getKey();

                    if (indicesByName.containsKey(companyName)) {
                        companyIdsGroupedByName.computeIfAbsent(companyEntry.getValue(), index -> new TreeSet<>()).addAll(indicesByName.get(companyName));
                    }
                }
            }
            return companyIdsGroupedByName;
        }
        return Collections.emptyMap();
    }

    /**
     * <p>
     * Groups the indices by existing contact ids retrieved using their emails and phones
     * </p>
     *
     * @param orgId          Represents the organization ID
     * @param indicesByEmail The map containing contact email as key and its index as value
     * @param indicesByPhone The map containing contact phone as key and its index as value
     * @return The map containing contact id as key and its respective indices as value
     */
    private Map<Long, Collection<Integer>> groupContactIdsByEmailPhone(final Long orgId, final Map<Object, Collection<Integer>> indicesByEmail,
                                                                       final Map<Object, Collection<Integer>> indicesByPhone) {
        final boolean isSourceRecordsGroupedByEmails = Objects.nonNull(indicesByEmail) && !indicesByEmail.isEmpty();
        final boolean isSourceRecordsGroupedByPhones = Objects.nonNull(indicesByPhone) && !indicesByPhone.isEmpty();
        final Collection<Long> emptyList = Collections.emptyList();
        final Map<Long, Collection<Integer>> contactIdsGroupedByEmailPhone = new HashMap<>();

        if (isSourceRecordsGroupedByEmails || isSourceRecordsGroupedByPhones) {
            final BiConsumer<Map<String, Long>, Map<Object, Collection<Integer>>> contactIdsConsumer = (groupedContactsByValues, indicesByValues) -> {

                for (final Map.Entry<String, Long> contactEntry : groupedContactsByValues.entrySet()) {
                    final String value = contactEntry.getKey();

                    if (indicesByValues.containsKey(value)) {
                        contactIdsGroupedByEmailPhone.computeIfAbsent(contactEntry.getValue(),
                                index -> new TreeSet<>()).addAll(indicesByValues.get(value));
                    }
                }
            };

            if (isSourceRecordsGroupedByEmails) {
                final List<String> emailAddresses = indicesByEmail.keySet().stream().filter(String.class::isInstance)
                        .map(String.class::cast).toList();
                final Map<String, Long> groupedContactIdsByEmails = new HashMap<>();
                final Collection<Collection<String>> batchedEmailsCollection = JavaUtil.splitCollection(emailAddresses, modelIdFetchBatchSize);

                for (final Collection<String> batchedEmails : batchedEmailsCollection) {
                    groupedContactIdsByEmails.putAll(contactManager.fetchContactIdsByEmails(orgId, batchedEmails, emptyList));
                }
                contactIdsConsumer.accept(groupedContactIdsByEmails, indicesByEmail);
            }

            if (isSourceRecordsGroupedByPhones) {
                final Collection<String> phoneNumbers = indicesByPhone.keySet().stream().filter(String.class::isInstance)
                        .map(String.class::cast).toList();
                final Map<String, Long> groupedContactIdsByPhoneNumbers = new HashMap<>();
                final Collection<Collection<String>> batchedPhonesCollection = JavaUtil.splitCollection(phoneNumbers, modelIdFetchBatchSize);

                for (final Collection<String> batchedPhones : batchedPhonesCollection) {
                    groupedContactIdsByPhoneNumbers.putAll(contactManager.fetchContactIdsByPhoneNumbers(orgId, batchedPhones, emptyList));
                }
                contactIdsConsumer.accept(groupedContactIdsByPhoneNumbers, indicesByPhone);
            }
            return contactIdsGroupedByEmailPhone;
        }
        return Collections.emptyMap();
    }

    /**
     * Loads the related source fields into the {@link SourceRecordContainer}
     *
     * @param sourceRecordContainer Which is to be loaded with the related source fields
     * @param mappedSources         Which contains the mapped sources in the import action
     * @param importFieldMap        Which contains the fields mapped with the alias names of the {@link FieldInfo}
     * @param fieldAliasName        Represents the related field alias name
     * @param relatedSource         Represents the related source
     */
    private void loadRelatedFields(final SourceRecordContainer sourceRecordContainer, final Collection<Source> mappedSources,
                                   final Map<String, FieldInfo> importFieldMap, final String fieldAliasName,
                                   final Source relatedSource) {

        if (mappedSources.contains(relatedSource)) {
            sourceRecordContainer.addRelatedField(new FieldSourcePair(importFieldMap.get(fieldAliasName), relatedSource));
        }
    }

    /**
     * <p>
     * Fetches and stores the model data containers
     * </p>
     *
     * @param orgId                Represents the organisation id
     * @param source               Represents the {@link Source}
     * @param sourceModelContainer Represents the {@link SourceModelContainer}
     * @param modelsToBeFetched    Represents the collection of ids of the model to be fetched
     * @param dataContainerFetcher Represents a bi function to get the related model container by applying related params
     */
    private void fetchAndStoreModelDataContainers(final Long orgId, final Source source, final SourceModelContainer sourceModelContainer,
                                                 final Collection<Long> modelsToBeFetched, final BiFunction<Long, Collection<Long>, DataContainer> dataContainerFetcher) {
        if (!modelsToBeFetched.isEmpty()) {
            LOGGER.debug("Data import: Model loading - Existing models are fetching. model IDs: {}, Org ID: {}", modelsToBeFetched, orgId);
            final long modelFetchStartTime = System.currentTimeMillis();
            final Collection<Collection<Long>> batchedModelIdsCollection = JavaUtil.splitCollection(modelsToBeFetched, modelDataFetchBatchSize);

            for (final Collection<Long> batchedModelIds : batchedModelIdsCollection) {
                LOGGER.debug("Data import: Model loading - models fetch by IDs: {}, Org ID: {}", batchedModelIds, orgId);
                final long modelsFetchStartTime = System.currentTimeMillis();
                sourceModelContainer.addSourceDataContainer(source, dataContainerFetcher.apply(orgId, batchedModelIds), batchedModelIds);
                LOGGER.debug("Data import: Model loading - models fetched by IDs are fetched in {}ms, IDs: {}, Org ID: {}",
                        System.currentTimeMillis() - modelsFetchStartTime, batchedModelIds, orgId);
            }
            LOGGER.debug("Data import: Model loading - Existing models are fetched in {} ms. model IDs: {}, Org ID: {}",
                    System.currentTimeMillis() - modelFetchStartTime, modelsToBeFetched, orgId);
        }
    }
}
package com.tlc.crm.core.fields.action;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.field.manager.DependableFieldsDataManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.fields.api.model.FieldVisibilityRule;
import com.tlc.crm.core.fields.builder.FieldVisibilityHandler;
import com.tlc.crm.core.fields.service.FieldVisibilityRuleService;
import com.tlc.crm.core.fields.api.model.DependableFieldsData;
import com.tlc.crm.entity.builder.FieldConstraintBuilder;
import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.internal.DropdownActionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.FieldTypeKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;

import com.tlc.crm.entity.models.converter.FieldVisibilityGroupConverter;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldConfig;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.service.dropdown.DropdownOptionReferenceService;
import com.tlc.crm.entity.util.ClientRegexUtil;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldStoreService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;
import com.tlc.crm.fields.manager.SummaryFieldPreferenceManager;
import com.tlc.crm.fields.models.SummaryFieldPreference;
import com.tlc.crm.security.permission.annotation.authorizer.FieldPermissionAuthorizer;
import com.tlc.crm.security.permission.annotation.authorizer.SourceAuthorizer;
import com.tlc.crm.security.permission.model.Action;
import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.I18nResolver;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.WebAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manages {@link Field} related web actions
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
@FieldPermissionAuthorizer()
@SourceAuthorizer(actions = {Action.Type.VIEW, Action.Type.ACCESS_RECYCLE_BIN}, relatedApiTypes = SourceAuthorizer.RelatedApiType.FIELD,
        needCommonApiHandling = true, needRelatedApiHandling = true)
@WebAction(path = "/fields")
public class FieldAction extends AuthorizationController {

    private static final long MAX_NUMBER_SIZE = 9999999999999999L;
    private static final int MAX_STRING_SIZE = 255;
    private static final int MAX_TEXT_SIZE = 32000;
    private static final int MAX_EMAIL_ADDRESS_SIZE = 320;
    private static final long MAX_MONETARY_VALUE = 9999999999999L;
    private static final int MIN_VALUE = 0;
    private static final int CUSTOM_FIELDS_SUMMARY_ROW_INDEX = 50;
    private static final String EMPTY_SPACE = StringUtil.Delimiter.EMPTY_SPACE.getDelimiter();
    private static final String INTERNAL_NAME_REGEX = "^cf_[a-z0-9]+(_[a-z0-9]+)*$";

    private final String phoneValidationI18NKey = "i18n.entity.fields.phone.format.invalid";
    private final String fieldNotFoundI18NKey = "i18n.entity.fields.not.found";
    private final DropdownActionContainer dropdownActionContainer;
    private final FieldService fieldService;
    private final CrmFieldsManager fieldManager;
    private final CrmFieldsManager secureFieldManager;
    private final FieldSourceService fieldSourceService;
    private final FieldStoreService fieldStoreService;
    private final OptionService optionService;
    private final FieldTypeService fieldTypeService;
    private final SourceSettingsService sourceSettingsService;
    private final OptionManager optionManager;
    private final FieldVisibilityGroupConverter fieldVisibilityGroupConverter;
    private final FieldVisibilityRuleService fieldVisibilityRuleService;
    private final ProductManager productManager;
    private final DependableFieldsDataManager dependableFieldsDataManager;
    private final FieldConstraintBuilder fieldConstraintBuilder;
    private final Collection<String> subscriptionFieldNames;
    private final SummaryFieldPreferenceManager summaryFieldPreferenceManager;
    private final DropdownOptionReferenceService dropdownOptionReferenceService;

    public FieldAction() {
        dropdownActionContainer = DropdownActionContainer.getInstance();
        fieldService = FieldService.getSecureInstance();
        fieldManager = CrmFieldsManager.getFieldsManager();
        secureFieldManager = CrmFieldsManager.getSecureFieldsManager();
        fieldSourceService = FieldSourceService.getInstance();
        fieldStoreService = FieldStoreService.getInstance();
        sourceSettingsService = SourceSettingsService.getInstance();
        optionService = OptionService.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        optionManager = OptionManager.getInstance();
        this.fieldVisibilityGroupConverter = FieldVisibilityGroupConverter.getInstance();
        this.fieldVisibilityRuleService = FieldVisibilityRuleService.getInstance();
        this.productManager = ProductManager.getInstance();
        this.dependableFieldsDataManager = DependableFieldsDataManager.getInstance();
        this.fieldConstraintBuilder = FieldConstraintBuilder.getInstance();
        subscriptionFieldNames = Set.of(ContactFields.SUBSCRIPTION_STATUS, ContactFields.SUBSCRIPTION_TYPES,
                ContactFields.UNSUBSCRIBE_REASON, ContactFields.OTHER_UNSUBSCRIBE_REASON);
        summaryFieldPreferenceManager = SummaryFieldPreferenceManager.getInstance();
        dropdownOptionReferenceService = DropdownOptionReferenceService.getInstance();
    }

    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final I18nResolver i18nResolver = request.i18nResolver();
        final I18nResolutionAdapter i18NResolutionAdapter = request.getI18NCRMResolutionAdapter();
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final String type = requestJson.getString(FieldKeys.TYPE.getName());

        switch (type) {
            case "get" -> {
                final Long id = data.getLong(FieldKeys.ID.getName());
                final Field field = secureFieldManager.get(id);

                field.setFieldConfig(secureFieldManager.getFieldConfig(orgId, field.id()));

                crmResponse.put(FieldKeys.FIELD.getName(), convertFieldToJsonObject(crmResponse, field));
                crmResponse.put(FieldKeys.SOURCE.getName(), convertEntitySourceToJsonObject(field.getSource()));
            }
            case "create" -> createField(orgId, data, crmResponse);
            case "bCreate" -> createFields(orgId, data, crmResponse);
            case "update" -> updateField(orgId, crmResponse, data);
            case "bUpdate" -> updateFields(crmResponse, orgId, data.getJsonArray(FieldKeys.LIST.getName()));
            case "delete" -> {
                final Long id = data.getLong(FieldKeys.ID.getName());

                delete(orgId, id);
                crmResponse.put(FieldKeys.ID.getName(), id);
                crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.entity.fields.delete.success"));
            }
            case "list" -> {
                final String entitySource = data.getString(FieldKeys.SOURCE.getName());
                final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(orgId, entitySource);

                if (fieldSource.isCommon()) {
                    if (fieldSource.getSettings().isEnabled()) {
                        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, entitySource);

                        if (fields.isEmpty()) {
                            throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, fieldNotFoundI18NKey);
                        } else {
                            crmResponse.put(FieldKeys.FIELDS.getName(), convertFieldsInfoToJsonArray(orgId, i18NResolutionAdapter, fields, data.optBoolean(FieldKeys.NEED_GROUP_VIEW.getName())));
                        }
                    }
                    crmResponse.put(FieldKeys.SOURCE.getName(), convertConfiguredSourceToJson(fieldSource));
                } else {
                    throw ErrorCode.get(EntityErrorCodes.ENTITY_TYPE_NOT_FOUND, "i18n.entity.type.not.found");
                }
            }
            case "dropdownData" -> fetchDropdownData(new DropdownRequest(request), crmResponse);
            case "fieldTypes" -> crmResponse.put(FieldKeys.LIST.getName(), getFieldTypes(i18nResolver, crmResponse));
            case "fieldType" -> crmResponse.put(FieldKeys.DATA.getName(), getFieldType(data, i18nResolver, crmResponse));
            case "formRender" -> dispatchFormRenderFields(orgId, false, data, request, crmResponse);
            case "updateOptionOrder" -> updateOptionsOrder(orgId, data, crmResponse);
            case "updateFieldOrder" -> updateFieldsOrder(orgId, data, crmResponse);
            case "updateFieldList" -> {
                final String entitySource = data.getString(FieldKeys.SOURCE.getName());
                final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, entitySource).stream().filter(fieldInfo ->
                        fieldInfo.presetConfig().isEditableViaList()).sorted(Comparator.comparingInt(fieldInfo -> fieldInfo.presetConfig().order())).toList();
                final Optional<FieldVisibilityHandler> optionalFieldVisibilityHandler = FieldVisibilityHandler.getFieldVisibilityHandler(Source.get(entitySource));

                if (fields.isEmpty()) {
                    throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, fieldNotFoundI18NKey);
                } else {
                    final boolean isPrimarySource = Source.isPrimarySource(Source.get(entitySource));

                    if (isPrimarySource) {
                        final JsonObject fieldSubTypeJson = buildSubFieldsJson(fields, false, i18nResolver, crmResponse, true);

                        if (fieldSubTypeJson.size() > 0) {
                            crmResponse.put(FieldKeys.SUB_FIELDS.getName(), fieldSubTypeJson);
                        }
                    }
                    final JsonArray fieldsJsonArray = Json.array();
                    final Collection<FieldInfo> updateFieldList = new ArrayList<>(fields);
                    final Map<Integer, List<FieldInfo>> groupedFields = fields.stream().filter(fieldInfo -> fieldInfo.presetConfig()
                            .isGroupedField()).collect(Collectors.groupingBy(fieldInfo -> fieldInfo.presetConfig().getGroupId()));

                    for (final List<FieldInfo> groupedFieldsInfo : groupedFields.values()) {
                        updateFieldList.removeAll(groupedFieldsInfo);

                        groupedFieldsInfo.stream().filter(fieldInfo -> fieldInfo.presetConfig().isFieldGroupHead()).findFirst().ifPresent(groupHeadField -> {
                            final JsonObject headFieldJson = convertFieldInfoIntoJsonObject(isPrimarySource, groupHeadField, crmResponse, i18NResolutionAdapter);
                            final JsonArray subfieldsArray = Json.array();

                            loadDependentFields(orgId, i18NResolutionAdapter, groupHeadField, subfieldsArray, groupedFieldsInfo, optionalFieldVisibilityHandler.orElseGet(() -> null));
                            if (0 < subfieldsArray.size()) {
                                headFieldJson.put(FieldKeys.SUB_FIELDS.getName(), subfieldsArray);
                            }
                            fieldsJsonArray.put(headFieldJson);
                        });
                    }


                    crmResponse.put(FieldKeys.FIELDS.getName(), convertFieldsIntoArray(isPrimarySource, updateFieldList, crmResponse, i18NResolutionAdapter).append(fieldsJsonArray));
                }
            }
            case "getDependableFields" -> {
                final String sourceName = data.getString(FieldKeys.SOURCE.getName());
                final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(orgId, sourceName);

                if (fieldSource.getSettings().isEnabled()) {
                    final Long fieldId = data.getLong(FieldKeys.ID.getName());
                    final FieldInfo fieldInfo = fieldService.getField(sourceName, fieldId);
                    final PresetConfig presetConfig = fieldInfo.presetConfig();
                    final int groupId = presetConfig.getGroupId();

                    if (0 < groupId) {
                        final Stream<FieldInfo> fieldStream = fieldService.getFieldsBySource(orgId, sourceName).stream()
                                .filter(field -> field.presetConfig().getGroupId() == groupId);

                        crmResponse.put(FieldKeys.FIELDS.getName(), buildFieldsWithVisibilityCriteria(orgId, fieldStream, i18NResolutionAdapter, Source.get(sourceName)));
                    } else {
                        throw ErrorCode.get(EntityErrorCodes.DEPENDABLE_FIELD_IS_NOT_AVAILABLE, "i18n.entity.fields.dependable.field.is.not.available");
                    }
                } else {
                    throw ErrorCode.get(EntityErrorCodes.ENTITY_SOURCE_IS_DISABLED, "i18n.entity.fields.source.is.disabled");
                }
            }
            case "getDependableFieldData" -> {
                final String sourceName = data.getString(FieldKeys.SOURCE.getName());
                final Long entityId = data.getLong(FieldKeys.ENTITY_ID.getName());
                final FieldInfo fieldInfo = fieldService.getField(sourceName, data.getLong(FieldKeys.FIELD_ID.getName()));
                final int groupId = fieldInfo.presetConfig().getGroupId();

                if (0 < groupId) {
                    final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, sourceName);
                    final Collection<FieldInfo> dependableFields = fields.stream()
                            .filter(field -> groupId == field.presetConfig().getGroupId()).collect(Collectors.toSet());
                    final DependableFieldsData dependableFieldsData = dependableFieldsDataManager.getDependableFieldsData(orgId,
                            entityId, Source.get(sourceName), dependableFields);

                    for (final Map.Entry<Long, Object> fieldDataEntry : dependableFieldsData.get().entrySet()) {
                        final Optional<Object> optionalValue = Optional.ofNullable(getDependableFieldJson(fieldDataEntry.getValue(), i18NResolutionAdapter));

                        optionalValue.ifPresent(value -> crmResponse.put(fieldDataEntry.getKey().toString(), value));
                    }
                    crmResponse.put(FieldKeys.ID.getName(), entityId);
                } else {
                    throw ErrorCode.get(EntityErrorCodes.DEPENDABLE_FIELD_IS_NOT_AVAILABLE, "i18n.entity.fields.dependable.field.is.not.available");
                }
            }
            case "getNameFieldId" -> {
                final Source source = Source.get(data.getString(FieldKeys.SOURCE.getName()));

                fieldService.groupDefaultFieldsByAliasName(source.getName())
                        .values().stream()
                        .filter(fieldInfo -> fieldInfo.presetConfig().isNameField())
                        .findFirst()
                        .ifPresent(fieldInfo -> crmResponse.put(FieldKeys.ID.getName(), fieldInfo.id()));
            }
            case "getSubscriptionFields" -> {
                final Stream<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(Contact.ENTITY_TYPE, subscriptionFieldNames)
                        .values().stream().sorted(Comparator.comparingInt(field -> field.presetConfig().order()));

                crmResponse.put(FieldKeys.FIELDS.getName(), buildFieldsWithVisibilityCriteria(orgId, fields, i18NResolutionAdapter, Source.CONTACT));
            }
            case "updateSummaryFieldPreference" -> updateSummaryFieldPreference(orgId, request.userId(), data);
            default -> {
                //handle(request, crmResponse);
            }
        }
    }

    /**
     * <p>
     * Builds the fields with visibility criteria
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param fields                Represents the stream of fields to be processed
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter} to resolve i18n strings
     * @param source                Represents the source for which the fields are being processed
     * @return the {@link JsonArray} containing the fields with visibility criteria
     */
    private JsonArray buildFieldsWithVisibilityCriteria(final Long orgId, final Stream<FieldInfo> fields, final I18nResolutionAdapter i18NResolutionAdapter, final Source source) {
        final JsonArray fieldsArray = Json.array();
        final Optional<FieldVisibilityHandler> fieldVisibilityHandler = FieldVisibilityHandler.getFieldVisibilityHandler(source);
        final boolean isFieldVisibilityHandlerEmpty = fieldVisibilityHandler.isEmpty();

        fields.forEach(field -> {
                    final Optional<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup;

                    if (isFieldVisibilityHandlerEmpty || field.isPersistent()) {
                        fieldVisibilityCriteriaGroup = Optional.empty();
                    } else {
                        fieldVisibilityCriteriaGroup = fieldVisibilityHandler.get().getCriteriaGroup(orgId, field);
                    }
                    final FieldVisibilityRule fieldVisibilityRule = fieldVisibilityCriteriaGroup
                            .map(criteriaGroup -> fieldVisibilityRuleService.getFieldVisibilityRule(field, criteriaGroup))
                            .orElseGet(() -> fieldVisibilityRuleService.getFieldVisibilityRule(field, new FieldVisibilityCriteriaGroup()));
                    final FieldVisibilityCriteriaGroup criteriaGroup = fieldVisibilityRule.fieldVisibilityCriteriaGroup();

                    if (Objects.nonNull(criteriaGroup)) {
                        fieldsArray.put(convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, fieldVisibilityRuleService.getFieldVisibilityRule(field, criteriaGroup), field.storeType(), true));
                    }
                }

        );

        return fieldsArray;
    }

    /**
     * <p>
     * Loads the dependent fields
     * </p>
     *
     * @param orgId                   Represents the organisation id
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @param groupHeadField          Represents the {@link FieldInfo} of group head field
     * @param subfieldsArray          Represents the {@link JsonArray} containing subfields
     * @param groupedFieldInfos       Represents the collection of grouped {@link FieldInfo}
     * @param fieldVisibilityHandler  Represents the {@link FieldVisibilityHandler}
     */
    private void loadDependentFields(final Long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final FieldInfo groupHeadField, final JsonArray subfieldsArray,
                                     final Collection<FieldInfo> groupedFieldInfos, final FieldVisibilityHandler fieldVisibilityHandler) {
        final String headFieldIdAsString = groupHeadField.idAsString();
        final Collection<FieldInfo> directSubFields = groupedFieldInfos.stream().filter(fieldInfo -> headFieldIdAsString.equals(fieldInfo.presetConfig().dependsOn())).toList();

        if (!directSubFields.isEmpty()) {
            final String criteriaKey = FieldKeys.CRITERIA.getName();

            for (final FieldInfo subField : directSubFields) {
                final FieldVisibilityRule fieldVisibilityRule;
                final String renderType;

                if (Objects.nonNull(fieldVisibilityHandler)) {
                    final Optional<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup = fieldVisibilityHandler.getCriteriaGroup(orgId, subField);

                    if (fieldVisibilityCriteriaGroup.isPresent()) {
                        fieldVisibilityRule = fieldVisibilityRuleService.getFieldVisibilityRule(subField, fieldVisibilityCriteriaGroup.get(), criteriaKey);
                        renderType = FieldKeys.CONDITIONAL.getName();
                    } else {
                        fieldVisibilityRule = fieldVisibilityRuleService.getFieldVisibilityRule(subField, new FieldVisibilityCriteriaGroup(), criteriaKey);
                        renderType = FieldKeys.DIRECT.getName();
                    }
                } else {
                    fieldVisibilityRule = fieldVisibilityRuleService.getFieldVisibilityRule(subField, new FieldVisibilityCriteriaGroup(), criteriaKey);
                    renderType = FieldKeys.DIRECT.getName();
                }
                final JsonObject subFieldJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, fieldVisibilityRule, subField.storeType(), true);
                final JsonObject configJson = subFieldJson.getJsonObject(FieldKeys.CONFIG.getName());

                if (configJson.containsKey(criteriaKey)) {
                    subFieldJson.put(criteriaKey, configJson.getJsonObject(criteriaKey));
                    configJson.remove(criteriaKey);
                }
                subFieldJson.put(FieldKeys.RENDER_TYPE.getName(), renderType);
                subfieldsArray.put(subFieldJson);
                final JsonArray subJsonArray = Json.array();

                loadDependentFields(orgId, i18NResolutionAdapter, subField, subJsonArray, groupedFieldInfos, fieldVisibilityHandler);

                if (0 < subJsonArray.size()) {
                    subFieldJson.put(FieldKeys.SUB_FIELDS.getName(), subJsonArray);
                }
            }
        }
    }

    /**
     * <p>
     * Converts a value to its JSON representation.
     * </p>
     *
     * @param value the value to convert
     * @return a {@code JsonObject} for a single pair
     */
    private Object getDependableFieldJson(final Object value, final I18nResolutionAdapter i18NResolutionAdapter) {
        if (value instanceof Pair<?, ?> valuePair) {
            return buildJsonObject((Long) valuePair.getKey(), valuePair.getValue().toString(), i18NResolutionAdapter);
        } else if (value instanceof Collection<?> valuePairs) {
            final Collection<Pair<Long, Object>> values = (Collection<Pair<Long, Object>>) valuePairs;
            final JsonArray jsonArray = Json.array();

            values.forEach(valuePair -> jsonArray.put(buildJsonObject(valuePair.getKey(), valuePair.getValue().toString(), i18NResolutionAdapter)));

            return jsonArray;
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof String stringValue) {
            return i18NResolutionAdapter.get(stringValue);
        }

        return null;
    }

    /**
     * <p>
     * Builds a JSON representation of a key-value pair.
     * </p>
     *
     * @param value the key of the pair
     * @param name  the value of the pair
     * @return a {@code JsonObject} containing the given key and value
     */
    private JsonObject buildJsonObject(final Long value, final String name, final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), value);
        jsonObject.put(FieldKeys.NAME.getName(), i18NResolutionAdapter.get(name));

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Handles the received request
     * @param crmResponse Generates the CRM response
     * @throws Exception if an error occurs during the request processing
     */
    @Override
    protected void handleMobileRequest(final CrmRequest crmRequest, final CrmResponse crmResponse) throws Exception {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = crmRequest.orgId();

        switch (type) {
            case "formRender" -> dispatchFormRenderFields(orgId, true, data, crmRequest, crmResponse);
            default -> authorizedProcess(crmRequest, crmResponse);
        }
    }

    /**
     * Gets the {@link JsonObject} which contains the field type data
     *
     * @param data        which contains the request data
     * @param crmResponse crm response
     * @return {@link JsonObject} which contains the field type data
     */
    private JsonObject getFieldType(final JsonObject data, final I18nResolver i18nResolver, final CrmResponse crmResponse) {
        final String typeIdKey = FieldTypeKeys.ID.getName();
        final String typeNameKey = FieldTypeKeys.NAME.getName();
        final Object value = data.containsKey(typeIdKey) ? data.getLong(typeIdKey) : data.opt(typeNameKey);

        if (Objects.nonNull(value)) {

            if (value instanceof final Long typeId) {
                return convertFieldTypeIntoJson(fieldTypeService.get(typeId), fieldTypeService.getSubTypesByFieldTypeId(typeId), i18nResolver, crmResponse);
            } else if (value instanceof final String typeName) {
                final StoreType storeType;

                try {
                    storeType = StoreType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    throw ErrorCode.get(EntityErrorCodes.INVALID_CRM_FIELD_STORE_TYPE, "i18n.entity.field.type.storeType.invalid");
                }

                final FieldType fieldType = fieldTypeService.getFieldTypeByStoreType(storeType);

                if (Objects.nonNull(fieldType)) {
                    return convertFieldTypeIntoJson(fieldType, fieldTypeService.getSubTypesByFieldTypeId(fieldType.id()),
                            i18nResolver, crmResponse);
                }
            }
        }

        return Json.object();
    }

    /**
     * Gets the fields by its configuration.
     *
     * @param orgId                Refers an organization id.
     * @param fieldSource          Refers the {@link FieldSource}.
     * @param fieldVisibilityRules Refers the collection of {@link FieldVisibilityRule}.
     * @return The collection of fields.
     */
    private Collection<FieldVisibilityRule> getConfiguredFieldVisibilityRules(final Long orgId, final FieldSource fieldSource, final Collection<FieldVisibilityRule> fieldVisibilityRules) {
        return switch (Source.valueOf(fieldSource.getName().toUpperCase())) {
            case CONTACT, COMPANY, PRODUCT -> fieldVisibilityRules;
            case DEAL -> fieldVisibilityRules.stream().peek(fieldVisibilityRule -> {
                final FieldInfo fieldInfo = fieldVisibilityRule.fieldInfo();

                if (fieldInfo.storeType() == StoreType.DEAL_VALUE) {
                    final PresetConfig presetConfig = fieldInfo.presetConfig();

                    presetConfig.add(PresetConfig.Configuration.IS_PRODUCT_ENABLED, sourceSettingsService
                            .isSourceEnabled(orgId, Source.PRODUCT.getName()));
                    presetConfig.add(PresetConfig.Configuration.PRODUCTS_AVAILABLE, productManager.doesOrgHaveProducts(orgId));
                }

            }).toList();
            default -> Collections.EMPTY_LIST;
        };
    }

    /**
     * Gets the collection of {@link FieldType}
     *
     * @param response crm response
     * @return {@link Collection} of {@link FieldType}
     */
    private JsonArray getFieldTypes(final I18nResolver i18nResolver, final CrmResponse response) {
        final JsonArray fieldTypeArray = Json.array();
        final Collection<FieldType> fieldTypes = fieldTypeService.getAllFieldTypes();

        if (Objects.nonNull(fieldTypes)) {
            fieldTypes.forEach(type -> fieldTypeArray.put(convertFieldTypeIntoJson(type,
                    fieldTypeService.getSubTypesByFieldTypeId(type.id()), i18nResolver, response)));
        }

        return fieldTypeArray;
    }

    /**
     * Converts the {@link FieldType} into json
     *
     * @param fieldType     which contains the field type data
     * @param fieldSubTypes which contains the {@link Collection} of field subtype
     * @param response      crm response
     * @return {@link JsonObject} which contains the field type data
     */
    private JsonObject convertFieldTypeIntoJson(final FieldType fieldType, final Collection<FieldSubType> fieldSubTypes,
            final I18nResolver i18nResolver, final CrmResponse response) {
        final JsonObject fieldTypeJson = Json.object();
        final String config = fieldType.config();

        fieldTypeJson.put(FieldTypeKeys.ID.getName(), fieldType.id());
        fieldTypeJson.put(FieldTypeKeys.NAME.getName(), fieldType.name());
        fieldTypeJson.put(FieldTypeKeys.DISPLAY_NAME.getName(), response.resolveI18nPlaceholder(fieldType.displayName()));

        if (Objects.nonNull(config)) {
            fieldTypeJson.put(FieldTypeKeys.CONFIG.getName(), Json.object(config));
        }

        if (StoreType.isSourceDependentType(fieldType.storeType())) {
            fieldTypeJson.put(FieldTypeKeys.IS_SOURCE_DEPENDENT.getName(), true);
        }

        if (Objects.nonNull(fieldSubTypes)) {
            final JsonArray fieldSubTypeArray = Json.array();

            fieldSubTypes.forEach(subType -> fieldSubTypeArray.put(convertFieldSubTypeIntoJson(subType, i18nResolver, response)));

            if (fieldSubTypeArray.size() > 0) {
                fieldTypeJson.put(FieldTypeKeys.SUB_TYPES.getName(), fieldSubTypeArray);
            }
        }

        return fieldTypeJson;
    }

    /**
     * Converts the {@link FieldSubType} into json
     *
     * @param subType  which contains the field subtype data
     * @param response crm response
     * @return {@link JsonObject} which contains the field subtype data
     */
    private JsonObject convertFieldSubTypeIntoJson(final FieldSubType subType, final I18nResolver i18nResolver, final CrmResponse response) {
        final JsonObject fieldSubTypeJson = Json.object();
        final String config = subType.config();

        fieldSubTypeJson.put(FieldTypeKeys.ID.getName(), subType.id());
        fieldSubTypeJson.put(FieldTypeKeys.NAME.getName(), subType.name());
        fieldSubTypeJson.put(FieldTypeKeys.TYPE.getName(), subType.fieldType().storeType().getName());
        fieldSubTypeJson.put(FieldTypeKeys.DISPLAY_NAME.getName(), response.resolveI18nPlaceholder(subType.displayName()));

        if (Objects.nonNull(config)) {
            fieldSubTypeJson.put(FieldTypeKeys.CONFIG.getName(), fieldConstraintBuilder.buildSubFields(Json.object(config), i18nResolver));
        }
        fieldSubTypeJson.put(FieldTypeKeys.INPUT_TYPE.getName(), subType.inputType());
        fieldSubTypeJson.put(FieldTypeKeys.PLACEHOLDER.getName(), response.resolveI18nPlaceholder(subType.placeholder()));

        return fieldSubTypeJson;
    }

    /**
     * <p>
     * Converts the {@link FieldSubType} into json
     * </p>
     *
     * @param fieldVisibilityRules Represents the collection of {@link FieldVisibilityRule}
     * @param isProfileEditView    A flag that indicating if the fields are being processed for a profile edit view
     * @param crmResponse          Represents the crm response
     * @param isAddView            A flag that indicating if the fields are being processed for an add view
     * @return {@link JsonObject}
     */
    private JsonObject getFieldSubTypeByGroup(final Collection<FieldVisibilityRule> fieldVisibilityRules, final boolean isProfileEditView,
            final I18nResolver i18nResolver, final CrmResponse crmResponse, final boolean isAddView) {
        final Collection<FieldInfo> fields = fieldVisibilityRules.stream().map(FieldVisibilityRule::fieldInfo).toList();

        return buildSubFieldsJson(fields, isProfileEditView, i18nResolver, crmResponse, isAddView);
    }

    /**
     * <p>
     * Builds the subfields {@link JsonObject}.
     * </p>
     *
     * @param fields            Holds the details of the field.
     * @param isProfileEditView A flag that indicating if the fields are being processed for a profile edit view
     * @param crmResponse       Holds the response data.
     * @param isAddView         A flag that indicating if the fields are being processed for an add view
     * @return Subfields {@link JsonObject}.
     */
    private JsonObject buildSubFieldsJson(final Collection<FieldInfo> fields, final boolean isProfileEditView, final I18nResolver i18nResolver, final CrmResponse crmResponse, final boolean isAddView) {
        final JsonObject jsonObject = Json.object();

        for (final FieldInfo field : fields) {
            final StoreType storeType = field.storeType();

            if (storeType.hasSubtypes() || storeType.hasSourceDependentFieldSubTypes()) {
                String typeName = storeType.getName();

                if (!jsonObject.containsKey(typeName)) {
                    final boolean isMonetaryType = storeType.isMonetaryType();
                    final JsonArray jsonArray = Json.array();

                    for (final FieldSubType fieldSubType : fieldTypeService.getSubTypesByStoreType(storeType)) {

                        if (isProfileEditView && isMonetaryType && StoreSubType.PRODUCT == fieldSubType.storeSubType()) {
                            continue;
                        }
                        final JsonObject subTypeJson = convertFieldSubTypeIntoJson(fieldSubType, i18nResolver, crmResponse);

                        if (!isAddView) {
                            final JsonObject configJson = subTypeJson.optJsonObject(FieldTypeKeys.CONFIG.getName());

                            if (Objects.nonNull(configJson)) {
                                configJson.remove(PresetConfig.Configuration.IS_DUPLICATE_CHECK_FIELD.getName());
                            }
                        }
                        jsonArray.put(subTypeJson);

                        if (isProfileEditView && isMonetaryType) {
                            typeName = StoreType.MONETARY.getName();
                        }
                    }
                    jsonObject.put(typeName, jsonArray);
                }
            }
        }
        return jsonObject;
    }

    /**
     * Fetches the results of option actions.
     *
     * @param dropdownRequest Holds the request needed to dropdown.
     * @param crmResponse     crmResponse
     */
    private void fetchDropdownData(final DropdownRequest dropdownRequest, final CrmResponse crmResponse) {
        final JsonObject data = dropdownRequest.data();

        if (data.containsKey(FieldKeys.ID.getName())) {
            final Long fieldId = data.getLong(FieldKeys.ID.getName());
            final FieldInfo field = fieldService.getField(dropdownRequest.getOrgId(), fieldId);
            final StoreType storeType = field.storeType();

            if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
                final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(field);

                loadOptions(dropdownRequest, field, dropdownAction, crmResponse);
            } else {
                throw ErrorCode.get(EntityErrorCodes.FIELD_IS_NOT_A_DROPDOWN, "i18n.entity.field.not.dropdown");
            }
        } else {
            final String name = data.getString(FieldKeys.NAME.getName());
            final AsyncDropdownAction asyncDropdownAction = dropdownActionContainer.fetchAsyncAction(name);

            if (Objects.isNull(asyncDropdownAction)) {
                final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(name);

                loadOptions(dropdownRequest, null, dropdownAction, crmResponse);
            } else {
                final int start = data.optInt(FieldKeys.START.getName(), 1);
                final int limit = data.optInt(FieldKeys.LIMIT.getName(), 100);
                final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
                final JsonObject criteriaJson = data.optJsonObject(FieldKeys.CRITERIA.getName());
                final DropdownCriteria dropdownCriteria = new DropdownCriteria();

                if (Objects.nonNull(criteriaJson)) {
                    final Object criteriaValue = criteriaJson.get(FieldKeys.VALUE.getName());
                    final String criteriaType = criteriaJson.getString(FieldKeys.TYPE.getName());

                    dropdownCriteria.setCriteriaValue(criteriaValue);
                    dropdownCriteria.setCriteriaType(criteriaType);
                }
                dropdownRequest.setLimit(dropdownLimit);
                dropdownRequest.setCriteria(dropdownCriteria);

                asyncDropdownAction.fetchAndDispatchData(dropdownRequest, crmResponse);
            }
        }
    }

    /**
     * Loads the dropdown Options.
     *
     * @param dropdownRequest Holds the request needed to dropdown.
     * @param field           represents the field related information.
     * @param dropdownAction  dropdown action
     * @param crmResponse     crmResponse
     */
    private void loadOptions(final DropdownRequest dropdownRequest, final FieldInfo field,
                             final DropdownAction dropdownAction, final CrmResponse crmResponse) {
        final OptionContainer optionContainer = fetchOptionContainer(dropdownRequest, field, dropdownAction);
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = optionContainer.getTotalRecords();

        crmResponse.put(FieldKeys.TOTAL_RECORDS.getName(), totalRecords);
        crmResponse.put(FieldKeys.HAS_MORE.getName(), dropdownLimit.start() + dropdownLimit.limit() <= totalRecords);
        crmResponse.put(FieldKeys.OPTIONS.getName(), optionContainer.getOptions());
        crmResponse.put(FieldKeys.SIZE.getName(), optionContainer.size());
    }

    /**
     * Fetches the {@link OptionContainer} for the requested dropdown values.
     *
     * @param dropdownRequest Holds the request needed to dropdown.
     * @param field           represents the field related information.
     * @param dropdownAction  dropdownAction.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    private OptionContainer fetchOptionContainer(final DropdownRequest dropdownRequest, final FieldInfo field, final DropdownAction dropdownAction) {
        final JsonObject data = dropdownRequest.data();
        final int start = data.optInt(FieldKeys.START.getName(), 1);
        final int limit = data.optInt(FieldKeys.LIMIT.getName(), 100);
        final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
        final JsonObject criteriaJson = data.optJsonObject(FieldKeys.CRITERIA.getName());
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        if (Objects.nonNull(criteriaJson)) {
            final Object criteriaValue = criteriaJson.get(FieldKeys.VALUE.getName());
            final String criteriaType = criteriaJson.getString(FieldKeys.TYPE.getName());

            dropdownCriteria.setCriteriaValue(criteriaValue);
            dropdownCriteria.setCriteriaType(criteriaType);
        }
        dropdownRequest.setLimit(dropdownLimit);
        dropdownRequest.setField(field);
        dropdownRequest.setCriteria(dropdownCriteria);

        if (CrmRequest.ClientType.MOBILE == dropdownRequest.getClientType()) {
            return dropdownAction.fetchMobileData(dropdownRequest);
        } else {
            return dropdownAction.fetchData(dropdownRequest);
        }
    }

    /**
     * <p>
     * Converts the given collection of field information objects into a JSON array.
     * </p>
     *
     * @param orgId                 The organization ID used for resolving field configuration
     * @param fields                The collection of field information objects to be converted
     * @param i18NResolutionAdapter The adapter used to resolve internationalized field labels
     * @return a {@link JsonArray} containing JSON representations of the provided field information
     */
    private JsonArray convertFieldsInfoToJsonArray(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<FieldInfo> fields,
                                                   final boolean isNeedGroupView) {
        final JsonArray jsonArray = Json.array();

        if (isNeedGroupView) {
            final Map<Integer, List<FieldInfo>> groupedDependableFields = fields.stream()
                    .collect(Collectors.groupingBy(field -> field.presetConfig().getGroupId()));
            final Set<Integer> groupIds = new HashSet<>();

            for (final FieldInfo fieldInfo : fields) {
                final int groupId = fieldInfo.presetConfig().getGroupId();

                if (groupId == 0) {
                    final JsonObject fieldInfoJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(fieldInfo), fieldInfo.storeType(), false);

                    jsonArray.put(Json.array().put(fieldInfoJson));
                } else if (!groupIds.contains(groupId)) {
                    final Collection<FieldInfo> dependableFields = groupedDependableFields.get(groupId);

                    if (Objects.nonNull(dependableFields)) {
                        final JsonArray dependableJsonArray = Json.array();

                        dependableFields.forEach(groupedField -> {
                            final JsonObject fieldInfoJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(groupedField), groupedField.storeType(), false);

                            dependableJsonArray.put(fieldInfoJson);
                        });
                        jsonArray.put(dependableJsonArray);
                    }
                    groupIds.add(groupId);
                }
            }
        } else {
            fields.forEach(fieldInfo -> {
                final JsonObject jsonObject = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(fieldInfo), fieldInfo.storeType(), false);

                jsonArray.put(jsonObject);
            });
        }
        return jsonArray;
    }

    /**
     * <p>
     * Converts the fields according to groups with respect to row index
     * </p>
     *
     * @param orgId                       Represents an organisation id
     * @param i18NResolutionAdapter     Represents the resolver of the I18N strings
     * @param fieldVisibilityRules        Represents {@link Collection} of {@link FieldVisibilityRule}
     * @param isAddView                   A flag that indicating if the fields are being processed for an add view
     * @param isProfileEditView           A flag that indicating if the fields are being processed for a profile edit view
     * @param includeDependableFieldGroup Represents whether the dependent fields need to be grouped into a single array or not
     * @return {@link JsonArray}
     */
    private JsonArray convertFieldsInfoInOrder(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<FieldVisibilityRule> fieldVisibilityRules,
                                               final boolean isAddView, final boolean isProfileEditView, final boolean includeDependableFieldGroup) {
        final JsonArray jsonArray = Json.array();
        final Stream<FieldInfo> fieldInfoStream = fieldVisibilityRules.stream().map(FieldVisibilityRule::fieldInfo);
        final Collection<FieldInfo> fieldList;

        if (isAddView) {
            fieldList = fieldInfoStream.filter(fieldInfo -> {
                final PresetConfig presetConfig = fieldInfo.presetConfig();

                return !presetConfig.readOnly() && !presetConfig.hidden();
            }).sorted(Comparator.comparing(fieldInfo -> fieldInfo.presetConfig().order())).toList();
        } else {
            fieldList = fieldInfoStream.sorted(Comparator.comparing(fieldInfo -> fieldInfo.presetConfig().order())).toList();
        }
        final Collection<FieldInfo> fields = sortDependableFields(fieldList);

        final Map<Long, FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup = fieldVisibilityRules.stream()
                .filter(Objects::nonNull).filter(fieldVisibilityRule -> Objects.nonNull(fieldVisibilityRule.fieldVisibilityCriteriaGroup()))
                .collect(Collectors.toMap(attribute -> attribute.fieldInfo().id(), FieldVisibilityRule::fieldVisibilityCriteriaGroup));
        final Map<Integer, List<FieldInfo>> fieldsGroupedByRowIndex = fields.stream().filter(fieldInfo -> fieldInfo.presetConfig().rowIndex() != -1)
                .collect(Collectors.groupingBy(fieldInfo -> fieldInfo.presetConfig().rowIndex()));

        if (includeDependableFieldGroup) {
            final Map<Integer, List<FieldInfo>> fieldsGroupedByGroupId = fields.stream().filter(fieldInfo -> fieldInfo.presetConfig().getGroupId() != 0)
                    .collect(Collectors.groupingBy(fieldInfo -> fieldInfo.presetConfig().getGroupId()));
            final Collection<FieldInfo> processedFields = new ArrayList<>();

            for (final FieldInfo field : fields) {
                if (!processedFields.contains(field)) {
                    final StoreType storeType = field.storeType();

                    if (isProfileEditView) {
                        jsonArray.put(convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(field, fieldVisibilityCriteriaGroup.get(field.id())), storeType.getBaseType(), true));
                    } else {
                        final PresetConfig presetConfig = field.presetConfig();
                        final int rowIndex = presetConfig.rowIndex();
                        final int groupId = presetConfig.getGroupId();

                        if (groupId != 0 && fieldsGroupedByGroupId.containsKey(groupId)) {
                            jsonArray.put(convertFieldsInfoInOrder(orgId, i18NResolutionAdapter, fieldVisibilityCriteriaGroup, fieldsGroupedByGroupId.remove(groupId), processedFields));
                        } else if (rowIndex == -1) {
                            final JsonArray fieldArray = Json.array();

                            fieldArray.put(convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter,
                                    new FieldVisibilityRule(field, fieldVisibilityCriteriaGroup.get(field.id())), storeType, true));
                            jsonArray.put(fieldArray);
                        } else if (fieldsGroupedByRowIndex.containsKey(rowIndex)) {
                            jsonArray.put(convertFieldsInfoInOrder(orgId, i18NResolutionAdapter, fieldVisibilityCriteriaGroup, fieldsGroupedByRowIndex.remove(rowIndex), processedFields));
                        }
                    }
                }
            }
        } else {
            convertFieldsInfoInOrder(orgId, jsonArray, isProfileEditView, fields, i18NResolutionAdapter, fieldVisibilityCriteriaGroup, fieldsGroupedByRowIndex, isAddView);
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link FieldInfo} into {@link JsonArray}
     * </p>
     *
     * @param orgId                        Represents an organization id
     * @param i18NResolutionAdapter      Represents the {@link I18nResolutionAdapter}
     * @param fieldVisibilityCriteriaGroup Represents the field visibility criteria group
     * @param fields                       Represents the {@link Collection} of {@link FieldInfo}
     * @param processedFields              Represents the {@link Collection} of processed fields
     * @return the {@link JsonArray} converted json array
     */
    private JsonArray convertFieldsInfoInOrder(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Map<Long, FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup,
                                               final Collection<FieldInfo> fields, final Collection<FieldInfo> processedFields) {
        final JsonArray jsonArray = Json.array();

        for (final FieldInfo fieldInfo : fields) {
            processedFields.add(fieldInfo);
            jsonArray.put(convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter,
                    new FieldVisibilityRule(fieldInfo, fieldVisibilityCriteriaGroup.get(fieldInfo.id())), fieldInfo.storeType(), true));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Sorts fields so that all fields with the same group ID appear next to each other,
     * ordered by their preset config order within each group.
     * </p>
     *
     * @param fields Represents the {@link Collection} of {@link FieldInfo}
     * @return the sorted {@link List} of {@link FieldInfo}
     */
    private List<FieldInfo> sortDependableFields(final Collection<FieldInfo> fields) {
        final List<FieldInfo> sortedFields = new ArrayList<>();
        final Map<Integer, List<FieldInfo>> groupedFields = fields.stream()
                .filter(field -> field.presetConfig().getGroupId() > 0)
                .collect(Collectors.groupingBy(field -> field.presetConfig().getGroupId()));
        final Collection<Integer> groupIds = new HashSet<>();

        for (final FieldInfo fieldInfo : fields) {
            final int groupId = fieldInfo.presetConfig().getGroupId();

            if (groupId > 0) {
                if (groupIds.add(groupId)) {
                    sortedFields.addAll(groupedFields.get(groupId));
                }
            } else {
                sortedFields.add(fieldInfo);
            }
        }
        return sortedFields;
    }

    /**
     * <p>
     * Loads the {@link FieldInfo} to {@link JsonArray}
     * </p>
     *
     * @param orgId                        Represents an organization id
     * @param jsonArray                    Represents the {@link JsonArray}
     * @param isProfileEditView            A flag that indicating if the fields are being processed for a profile edit view
     * @param fields                       Represents the {@link Collection} of {@link FieldInfo}
     * @param i18NResolutionAdapter      Represents the {@link I18nResolutionAdapter}
     * @param fieldVisibilityCriteriaGroup Represents the field visibility criteria group
     * @param groupedFields                Represents the {@link Map} of grouped fields
     * @param isAddView                    A flag that indicating if the fields are being processed for an add view
     */
    private void convertFieldsInfoInOrder(final long orgId, final JsonArray jsonArray, final boolean isProfileEditView, final Collection<FieldInfo> fields,
                                          final I18nResolutionAdapter i18NResolutionAdapter, final Map<Long, FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup,
                                          final Map<Integer, List<FieldInfo>> groupedFields, final boolean isAddView) {
        for (final FieldInfo field : fields) {
            final StoreType storeType = field.storeType();

            if (isProfileEditView) {
                jsonArray.put(convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, fieldVisibilityRuleService.getFieldVisibilityRule(field, fieldVisibilityCriteriaGroup.get(field.id())), storeType.isMonetaryType() ? StoreType.MONETARY : storeType, true));
            } else {
                final int rowIndex = field.presetConfig().rowIndex();

                if (-1 == rowIndex) {
                    final JsonArray fieldArray = Json.array();
                    final JsonObject fieldJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter,
                            fieldVisibilityRuleService.getFieldVisibilityRule(field, fieldVisibilityCriteriaGroup.get(field.id())), field.storeType(), true);

                    if (!isAddView) {
                        fieldJson.getJsonObject(FieldKeys.CONFIG.getName()).remove(PresetConfig.Configuration.IS_DUPLICATE_CHECK_FIELD.getName());
                    }

                    fieldArray.put(fieldJson);
                    jsonArray.put(fieldArray);
                } else if (groupedFields.containsKey(rowIndex)) {
                    final JsonArray fieldArray = Json.array();

                    groupedFields.remove(rowIndex).stream().map(groupedField -> {
                        final JsonObject fieldJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter,
                                new FieldVisibilityRule(groupedField, fieldVisibilityCriteriaGroup.get(groupedField.id())),
                                groupedField.storeType(), true);

                        if (!isAddView) {
                            fieldJson.getJsonObject(FieldKeys.CONFIG.getName()).remove(PresetConfig.Configuration.IS_DUPLICATE_CHECK_FIELD.getName());
                        }
                        return fieldJson;
                    }).forEach(fieldArray::put);
                    jsonArray.put(fieldArray);
                }
            }
        }
    }

    /**
     * <p>
     * Converts the {@link FieldInfo} to {@link JsonObject}
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @param fieldVisibilityRule     Represents the {@link FieldVisibilityRule}
     * @param storeType               Represents the {@link StoreType}
     * @param needValidationConfig    Indicates whether the validation config is required ot not
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldInfoToJsonObject(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final FieldVisibilityRule fieldVisibilityRule,
                                                    final StoreType storeType, final boolean needValidationConfig) {
        final JsonObject jsonObject = Json.object();
        final FieldInfo field = fieldVisibilityRule.fieldInfo();
        final PresetConfig presetConfig = field.presetConfig();
        final JsonObject presetConfigJson = presetConfig.copyPresetConfigJson();
        final Long sourceId = field.sourceId();
        final FieldVisibilityCriteriaGroup fieldVisibilityCriteriaGroup = fieldVisibilityRule.fieldVisibilityCriteriaGroup();
        final I18nKey placeHolder = field.getPlaceHolder();
        final String referrerFieldsKey = PresetConfig.Configuration.REFERRER_FIELDS.getName();
        final BiConsumer<String, String> relatedFieldConsumer = (source, aliasName) -> {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(source, aliasName);
            final JsonObject relatedFieldJson = Json.object();

            relatedFieldJson.put(FieldKeys.ID.getName(), fields.get(aliasName).id());
            relatedFieldJson.put(FieldKeys.SOURCE.getName(), source);
            jsonObject.put(FieldKeys.RELATED_FIELD.getName(), relatedFieldJson);
        };

        if (Objects.nonNull(fieldVisibilityCriteriaGroup)) {
            fieldVisibilityGroupConverter.loadCriteriaGroupIntoJson(orgId, presetConfigJson, fieldVisibilityCriteriaGroup, i18NResolutionAdapter, fieldVisibilityRule.fieldVisibilityKey());
        }

        if (presetConfigJson.containsKey(referrerFieldsKey)) {
            final Collection<String> fieldNames = new ArrayList<>();

            presetConfigJson.getJsonArray(referrerFieldsKey).forEach(fieldName -> fieldNames.add((String) fieldName));
            final Map<String, FieldInfo> fieldsByAliasName = fieldService.groupDefaultFieldsByAliasName(field.fieldSource().getName(), fieldNames);
            final JsonArray referrerFieldsArray = Json.array();
            final String fieldsKey = FieldKeys.ID.getName();
            final String fieldTypeKey = FieldKeys.FIELD_TYPE.getName();

            for (final String fieldName : fieldNames) {
                final FieldInfo fieldInfo = fieldsByAliasName.get(fieldName);

                if (Objects.nonNull(fieldInfo)) {
                    referrerFieldsArray.put(Json.object().put(fieldsKey, fieldInfo.id()).put(fieldTypeKey, fieldInfo.storeType()));
                }
            }

            presetConfigJson.remove(referrerFieldsKey);
            jsonObject.put(referrerFieldsKey, referrerFieldsArray);
            jsonObject.put(FieldKeys.TYPE.getName(), StoreType.IDENTIFIER.name());
        } else {
            jsonObject.put(FieldKeys.TYPE.getName(), storeType.name());
        }
        presetConfigJson.remove(PresetConfig.Configuration.ROW_INDEX.getName());
        presetConfigJson.remove(PresetConfig.Configuration.ROW_INDEX_ON_VIEW.getName());
        presetConfigJson.remove(PresetConfig.Configuration.ORDER_ON_VIEW.getName());
        presetConfigJson.remove(PresetConfig.Configuration.SHOW_ON_PROFILE.getName());
        presetConfigJson.remove(PresetConfig.Configuration.PLACE_HOLDER.getName());
        presetConfigJson.remove(PresetConfig.Configuration.HAS_MAPPER_TABLE.getName());
        presetConfigJson.remove(PresetConfig.Configuration.FILTER_DROPDOWN.getName());
        jsonObject.put(FieldKeys.SOURCE_ID.getName(), sourceId);
        jsonObject.put(FieldKeys.ALIAS_NAME.getName(), field.aliasName());
        jsonObject.put(FieldKeys.FIELD_NAME.getName(), i18NResolutionAdapter.get(field.i18nName()));
        jsonObject.put(FieldKeys.INTERNAL_NAME.getName(), i18NResolutionAdapter.get(field.getInternalName()));
        jsonObject.put(FieldKeys.IS_ACTIVE.getName(), field.isActive());
        jsonObject.put(FieldKeys.CONFIG.getName(), presetConfigJson);
        jsonObject.put(FieldKeys.ORG_ID.getName(), field.orgId());

        if (Source.ACTIVITY.getName().equalsIgnoreCase(fieldSourceService.getSource(sourceId).getName())) {

            if (presetConfigJson.containsKey(FieldKeys.DEPENDS_ON.getName())) {
                presetConfigJson.put(FieldKeys.DEPENDS_ON.getName(), fieldService.getDefaultFieldById(presetConfigJson.getLong(FieldKeys.DEPENDS_ON.getName())).aliasName());
            }
            jsonObject.put(FieldKeys.ID.getName(), field.aliasName());
        } else {
            jsonObject.put(FieldKeys.ID.getName(), field.id());
        }

        if (Objects.nonNull(placeHolder)) {
            jsonObject.put(FieldTypeKeys.PLACEHOLDER.getName(), i18NResolutionAdapter.get(placeHolder.getKey()));
        }
        presetConfig.getMaxValue().ifPresent(value -> presetConfigJson.put(FieldKeys.MAX_DATE.getName(), value));

        if (needValidationConfig) {
            final JsonObject validationJson = fieldConstraintBuilder.build(field, presetConfigJson, i18NResolutionAdapter);

            if (0 < validationJson.size()) {
                presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
            }
        }

        switch (storeType) {
            case CONTACT_ONBOARD, CONTACTS_ONBOARD -> relatedFieldConsumer.accept(Source.CONTACT.getName(),
                    FieldKeys.FIRST_NAME.getName());
            case COMPANY_ONBOARD -> relatedFieldConsumer.accept(Source.COMPANY.getName(), FieldKeys.NAME.getName());
            case DEAL_VALUE -> relatedFieldConsumer.accept(Source.PRODUCT.getName(), FieldKeys.NAME.getName());
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts entity source into json object
     * </p>
     *
     * @param fieldSource Holds source information
     * @return {@link JsonObject}
     */
    private JsonObject convertEntitySourceToJsonObject(final FieldSource fieldSource) {
        final JsonObject source = Json.object();

        source.put(FieldKeys.NAME.getName(), fieldSource.getName());
        source.put(FieldKeys.ID.getName(), fieldSource.id());

        return source;
    }

    /**
     * Converts the field source with its configuration to json.
     *
     * @param fieldSource Refers the field source.
     * @return The {@link JsonObject}
     */
    private JsonObject convertConfiguredSourceToJson(final FieldSource fieldSource) {
        final JsonObject source = Json.object();
        final SourceSettings sourceSettings = fieldSource.getSettings();
        final boolean isEnabled = sourceSettings.isEnabled();
        final String sourceConfig = sourceSettings.getConfig();

        source.put(FieldKeys.NAME.getName(), fieldSource.getName());
        source.put(FieldKeys.ID.getName(), fieldSource.id());
        source.put(FieldKeys.IS_ENABLED.getName(), isEnabled);

        if (isEnabled && null != sourceConfig) {
            source.put(FieldKeys.CONFIG.getName(), Json.object(sourceConfig));
        }

        return source;
    }

    /**
     * <p>
     * Converts {@link Field} to {@link JsonObject}
     * </p>
     *
     * @param field Holds field information
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldToJsonObject(CrmResponse crmResponse, final Field field) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.SOURCE_ID.getName(), field.getSource().id());
        jsonObject.put(FieldKeys.FIELD_NAME.getName(), crmResponse.resolveI18nString(field.getI18nName()));
        jsonObject.put(FieldKeys.INTERNAL_NAME.getName(), field.getInternalName());
        jsonObject.put(FieldKeys.TYPE.getName(), field.getFieldType().name());
        jsonObject.put(FieldKeys.IS_ACTIVE.getName(), field.getActive());
        jsonObject.put(FieldKeys.CONFIG.getName(), Json.object(field.getFieldConfig().getPresetConfig()));
        jsonObject.put(FieldKeys.ID.getName(), field.id());
        jsonObject.put(FieldKeys.ORG_ID.getName(), field.orgId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts collection of crm entity into array of json objects i.e {@link JsonArray}
     * </p>
     *
     * @param crmResponse Holds the response data
     * @param fields      Holds the information of multiple entity
     * @return {@link JsonArray}
     */
    private JsonArray convertFieldsToJsonArray(final CrmResponse crmResponse, final Collection<Field> fields) {
        final JsonArray jsonArray = Json.array();

        if (Objects.nonNull(fields) && !fields.isEmpty()) {
            fields.forEach(field -> jsonArray.put(convertFieldToJsonObject(crmResponse, field)));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Disables the field that has the given field id
     * </p>
     *
     * @param orgId   Organization id
     * @param fieldId Field id
     */
    void delete(final Long orgId, final Long fieldId) {
        final FieldInfo field = fieldService.getField(orgId, fieldId);

        if (Objects.nonNull(field) && field.isPersistent()) {
            secureFieldManager.disableField(orgId, fieldId);
        } else {
            throw ErrorCode.get(EntityErrorCodes.FIELD_DELETE_RESTRICTED, "i18n.entity.field.delete.restricted");
        }
    }

    /**
     * <p>
     * Updates the field information represented in the {@link JsonObject}
     * </p>
     *
     * @param orgId       Organization id
     * @param crmResponse CRM response wrapper
     * @param data        Holds field information
     */
    void updateField(final Long orgId, final CrmResponse crmResponse, final JsonObject data) {
        final JsonArray jsonArray = Json.array();

        jsonArray.put(data);
        updateFields(crmResponse, orgId, jsonArray);
    }

    /**
     * <p>
     * Updates the field information represented in the {@link JsonArray}
     * </p>
     *
     * @param crmResponse CRM response wrapper
     * @param orgId       Organization id
     * @param jsonFields  Holds multiple entity information
     */
    void updateFields(final CrmResponse crmResponse, final Long orgId, final JsonArray jsonFields) {
        final Collection<Long> ids = jsonFields.toList().stream()
                .map(jsonField -> (Long) ((Map<String, Object>) jsonField).get(FieldKeys.ID.getName())).toList();
        final Map<Long, Field> existingFields = fieldManager.get(orgId, ids);
        final Map<Long, Field> fields = new HashMap<>();

        jsonFields.forEach(fieldMap -> {
            final JsonObject jsonField = Json.object((Map<String, Object>) fieldMap);
            final Long fieldId = jsonField.getLong(FieldKeys.ID.getName());

            if (existingFields.containsKey(fieldId)) {
                convertField(orgId, existingFields.get(fieldId), jsonField, fields, crmResponse);
            }
        });
        final Collection<Field> fieldsToBeUpdated = fields.values();

        validateFieldNames(false, orgId, fieldsToBeUpdated, crmResponse);
        handleMutableDropdownOptionRemoval(orgId, jsonFields, existingFields);
        fieldManager.update(orgId, fieldsToBeUpdated);
        crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.entity.fields.update.success"));
    }

    /**
     * <p>
     * Reassigns entity dropdown values for removed mutable options before {@code fieldManager.update} deletes
     * option rows (avoids {@code ON-DELETE-CASCADE} on mapper {@code OPTION_ID}).
     * </p>
     *
     * @param orgId          Organization id.
     * @param jsonFields     Field update payloads from {@code update} or {@code bUpdate}.
     * @param existingFields Existing fields keyed by field id.
     */
    private void handleMutableDropdownOptionRemoval(final Long orgId, final JsonArray jsonFields,
                                                    final Map<Long, Field> existingFields) {
        final String configKey = FieldKeys.CONFIG.getName();
        final String optionsKey = FieldKeys.OPTIONS.getName();

        jsonFields.forEach(fieldMap -> {
            final JsonObject jsonField = Json.object((Map<String, Object>) fieldMap);
            final Long fieldId = jsonField.getLong(FieldKeys.ID.getName());

            if (existingFields.containsKey(fieldId)) {
                final Field field = existingFields.get(fieldId);
                final JsonObject config = jsonField.optJsonObject(configKey);

                if (Objects.nonNull(config) && config.containsKey(optionsKey) && isMutableDropdownField(field)) {
                    final Collection<Long> removedOptionIds = getRemovedEditableOptionIds(orgId, fieldId, config);

                    if (!removedOptionIds.isEmpty()) {
                        dropdownOptionReferenceService.reassign(orgId, fieldId, removedOptionIds, config);
                    }
                }
            }
        });
    }

    /**
     * <p>
     * Finds editable option ids stored for the field that are absent from the update request options list.
     * </p>
     *
     * @param orgId      Organization id.
     * @param fieldId    Field id.
     * @param configJson Field config from the update request.
     * @return Removed editable option ids; empty when reassignment does not apply.
     */
    private Collection<Long> getRemovedEditableOptionIds(final Long orgId, final Long fieldId, final JsonObject configJson) {
        final String optionsKey = FieldKeys.OPTIONS.getName();
        final JsonArray optionArray = configJson.optJsonArray(optionsKey);

        if (Objects.isNull(optionArray)) {
            return List.of();
        }
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
        final String valueKey = FieldKeys.VALUE.getName();
        final Set<Long> retainedOptionIds = new HashSet<>();
        final int optionSize = optionArray.size();

        for (int count = NumericalConstant.ZERO; count < optionSize; count++) {
            final JsonObject optionJson = optionArray.getJsonObject(count);
            final Long optionId = getOptionIdFromRequest(optionJson, valueKey);

            if (Objects.nonNull(optionId)) {
                retainedOptionIds.add(optionId);
            }
        }

        return optionService.get(orgId, fieldInfo).stream().filter(Option::isEditable).map(Option::id).
                filter(optionId -> !retainedOptionIds.contains(optionId)).toList();
    }

    /**
     * <p>
     * Whether the field is a mutable {@link StoreType#DROPDOWN} (HYBRID/DYNAMIC).
     * </p>
     */
    private boolean isMutableDropdownField(final Field field) {

        if (Objects.isNull(field) || !Objects.equals(StoreType.DROPDOWN, field.getStore().storeType())) {
            return false;
        }

        final PresetConfig presetConfig = new PresetConfig(Json.object(field.getFieldConfig().getPresetConfig()));
        final DropdownType dropdownType = presetConfig.dropdownType();

        return Objects.nonNull(dropdownType) && dropdownType.isMutable();
    }

    /**
     * <p>
     * Reads a persisted option id from field-update option JSON ({@code value} or {@code id}).
     * </p>
     */
    private Long getOptionIdFromRequest(final JsonObject optionJson, final String valueKey) {

        if (optionJson.containsKey(valueKey)) {
            return optionJson.getLong(valueKey);
        }

        if (optionJson.containsKey(OptionKeys.ID)) {
            return optionJson.getLong(OptionKeys.ID);
        }

        return null;
    }

    /**
     * <p>
     * Converts the given {@link JsonObject} into an updated {@link Field}, validating the incoming
     * config, propagating changes to any dependent/related fields, and merging the result(s) into the
     * supplied {@code fields} map so they can be handed off for persistence — the same way
     * {@code convertToCrmField} produces a {@link Field} that is later passed into {@code createFields}.
     * </p>
     *
     * @param orgId       Organization id
     * @param field       Represents the existing {@link Field} being updated
     * @param jsonField   Holds the incoming field update data
     * @param fields      Represents the fields grouped by id, populated with the converted field(s)
     * @param crmResponse CRM response wrapper
     */
    private void convertField(final Long orgId, final Field field, final JsonObject jsonField, final Map<Long, Field> fields, final CrmResponse crmResponse) {
        final String addViewKey = FieldKeys.ADD_VIEW.getName();
        final String requiredKey = FieldKeys.REQUIRED.getName();
        final String configKey = FieldKeys.CONFIG.getName();
        final String isHiddenKey = FieldKeys.IS_HIDDEN.getName();
        final String maxKey = FieldKeys.MAX.getName();
        final Long fieldId = field.id();
        final FieldConfig fieldConfig = field.getFieldConfig();
        final boolean persistent = field.isPersistent();
        final JsonObject currentConfig = jsonField.getJsonObject(configKey);
        final PresetConfig presetConfig = new PresetConfig(Json.object(fieldConfig.getPresetConfig()));
        final boolean required = currentConfig.optBoolean(requiredKey);
        final int groupId = presetConfig.getGroupId();
        final boolean containsIsHiddenKey = currentConfig.containsKey(isHiddenKey);

                if (containsIsHiddenKey && currentConfig.getBoolean(isHiddenKey)) {
                    currentConfig.put(addViewKey, false);
                    currentConfig.put(requiredKey, false);
                }
                final boolean containsAddViewKey = currentConfig.containsKey(addViewKey);
                final boolean containsRequiredKey = currentConfig.containsKey(requiredKey);

                if (groupId != 0 && (containsIsHiddenKey || containsAddViewKey || containsRequiredKey)) {
                    fieldManager.getFieldsBySource(orgId, field.getSource().id()).stream().filter(existingField -> {
                        final PresetConfig existingPresetConfig = new PresetConfig(Json.object(existingField.getFieldConfig().getPresetConfig()));

                        return !Objects.equals(existingField.id(), field.id())
                                && groupId == existingPresetConfig.getGroupId();
                    }).forEach(dependentField -> {
                        final FieldConfig dependentFieldConfig = dependentField.getFieldConfig();
                        final JsonObject configJson = Json.object();

                        if (containsIsHiddenKey) {
                            configJson.put(isHiddenKey, currentConfig.get(isHiddenKey));
                        }

                        if (containsAddViewKey) {
                            configJson.put(addViewKey, currentConfig.get(addViewKey));
                        }

                        if (containsRequiredKey) {
                            if (required) {
                                configJson.put(addViewKey, true);
                            }
                            configJson.put(requiredKey, currentConfig.get(requiredKey));
                        }
                        dependentFieldConfig.setPresetConfig(configJson.toString());
                        dependentField.setFieldConfig(dependentFieldConfig);
                        dependentField.setOrgId(orgId);
                        mergeFieldConfig(fields, dependentField);
                    });
                }

                if (Objects.isNull(field.orgId())) {
                    field.setOrgId(orgId);
                }

                if (isValidDetails(field, currentConfig, presetConfig, jsonField, crmResponse)) {
                    int changesIncrementer = NumericalConstant.ZERO;
                    final JsonObject updatedConfig = Json.object();
                    final DropdownType dropdownType = presetConfig.dropdownType();

                    if (required) {
                        currentConfig.put(addViewKey, true);
                    }

                    if (persistent) {
                        final String currentName = jsonField.optString(FieldKeys.FIELD_NAME.getName(), field.getName());

                        if (!Objects.equals(currentName, field.getName())) {
                            changesIncrementer++;
                            field.setName(currentName);
                        }
                    }

                    if (Objects.nonNull(dropdownType)) {
                        modifyOptions(orgId, fieldId, dropdownType, currentConfig);
                    }

                    final JsonObject oldConfigAsJson = presetConfig.presetConfigAsJson();

                    if (currentConfig.containsKey(maxKey)) {
                        final long updateMax = currentConfig.getLong(maxKey);

                        if (updateMax > MAX_NUMBER_SIZE) {
                            currentConfig.put(maxKey, MAX_NUMBER_SIZE);
                        }
                    }

                    updatedConfig.append(currentConfig);

                    if (!currentConfig.toString().equals(oldConfigAsJson.toString())) {
                        changesIncrementer++;

                        fieldConfig.setPresetConfig(updatedConfig.toString());

                        field.setFieldConfig(fieldConfig);
                    }

                    if (changesIncrementer < 1) {
                        throw ErrorCode.get(EntityErrorCodes.NO_FIELD_MODIFICATION_IDENTIFIED,"i18n.entity.fields.no.modification.identified");
                    }
                }

                if (!persistent) {
                    final String emailsKey = FieldKeys.EMAILS.getName();
                    final String phonesKey = FieldKeys.PHONES.getName();
                    final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(orgId, Source.CONTACT.getName(), List.of(emailsKey, phonesKey));

                    if (contactFields.values().stream().map(FieldInfo::id).toList().contains(fieldId) && currentConfig.containsKey(requiredKey)) {
                        final Long emailFieldId = contactFields.get(emailsKey).id();
                        final Long dependentFieldId = Objects.equals(fieldId, emailFieldId) ? contactFields.get(phonesKey).id() : emailFieldId;
                        final Field dependentField = fieldManager.get(orgId, List.of(dependentFieldId)).get(dependentFieldId);
                        final FieldConfig dependentFieldConfig = dependentField.getFieldConfig();
                        final JsonObject updatedConfig = Json.object();

                        updatedConfig.put(FieldKeys.IMMUTABLE.getName(), !required);
                        dependentFieldConfig.setPresetConfig(updatedConfig.toString());
                        dependentField.setFieldConfig(dependentFieldConfig);
                        dependentField.setOrgId(orgId);
                        mergeFieldConfig(fields, dependentField);
                    }
                }
                mergeFieldConfig(fields, field);
    }

    /**
     * <p>
     * Merges the field config if field present in the field map, otherwise it adds the field to the map.
     * </p>
     *
     * @param fields Represents the fields grouped by the id.
     * @param field  Represents the {@link Field}
     */
    private void mergeFieldConfig(final Map<Long, Field> fields, final Field field) {
        final Long fieldId = field.id();

        if (fields.containsKey(fieldId)) {
            final FieldConfig fieldConfig = field.getFieldConfig();
            final FieldConfig existingFieldConfig = fields.get(fieldId).getFieldConfig();
            final JsonObject existingConfigJson = Json.object(existingFieldConfig.getPresetConfig());

            existingConfigJson.append(Json.object(fieldConfig.getPresetConfig()));
            fieldConfig.setPresetConfig(existingConfigJson.toString());
            fields.put(fieldId, field);
        } else {
            fields.put(fieldId, field);
        }
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link Field}.
     * </p>
     *
     * @param orgId        Organization's id.
     * @param fieldName    Field's name.
     * @param internalName Internal name of the field.
     * @param storeType    {@link StoreType}.
     * @param fieldSource  {@link FieldSource}.
     * @param config       Field's config.
     * @return {@link Field}.
     */
    Field convertToCrmField(final Long orgId, final String fieldName, final String internalName, final StoreType storeType,
                            final FieldSource fieldSource, final JsonObject config) {
        final Field field = new Field();
        final FieldConfig fieldConfig = new FieldConfig();
        final String requiredKey = FieldKeys.REQUIRED.getName();
        final String dropdownTypeKey = FieldKeys.DROPDOWN_TYPE.getName();
        final String addViewKey = FieldKeys.ADD_VIEW.getName();
        final String maxKey = FieldKeys.MAX.getName();
        final String minKey = FieldKeys.MIN.getName();
        final String maxLengthKey = FieldKeys.MAX_LENGTH.getName();

        field.setSource(fieldSource);
        field.setFieldType(fieldTypeService.getFieldTypeByStoreType(storeType));
        field.setStore(fieldStoreService.getFieldStoreByType(field.getSource().getName(), storeType, true));
        field.setOrgId(orgId);
        field.setName(fieldName);
        field.setActive(true);
        field.setInternalName(internalName);
        config.put(addViewKey, config.optBoolean(addViewKey));

        if (config.optBoolean(requiredKey)) {
            config.put(addViewKey, true);
            config.put(requiredKey, true);
        }
        config.put(PresetConfig.Configuration.REQUIRED_MESSAGE.getName(), "i18n.fields.violation.message.required");

        switch (storeType) {
            case DROPDOWN, MULTI_SELECT -> {
                if (!config.containsKey(dropdownTypeKey)) {
                    config.put(dropdownTypeKey, DropdownType.DYNAMIC.name());
                }
            }
            case PHONE -> {
                final JsonObject validationJson = Json.object();

                validationJson.put(FieldKeys.MESSAGE.getName(), phoneValidationI18NKey);
                validationJson.put(FieldKeys.VALUE.getName(), ClientRegexUtil.getRegex(storeType.getName()));

                config.put(FieldKeys.PATTERN.getName(), validationJson);
            }
            case EMAIL -> {
                config.put(FieldKeys.MAX_LENGTH.getName(), MAX_EMAIL_ADDRESS_SIZE);
                config.put(PresetConfig.Configuration.MAX_LENGTH_MESSAGE.getName(), "i18n.fields.violation.message.max.length");
            }
            case SINGLE_NUMBER -> {
                if (!config.containsKey(maxKey)) {
                    config.put(maxKey, MAX_NUMBER_SIZE);
                    config.put(minKey, MIN_VALUE);
                    config.put(PresetConfig.Configuration.MIN_MESSAGE.getName(), "i18n.fields.violation.message.min");
                    config.put(PresetConfig.Configuration.MAX_MESSAGE.getName(), "i18n.fields.violation.message.max");
                }
            }
            case AUTO_NUMBER -> {
                final String numberKey = FieldKeys.START_NUMBER.getName();
                final String noOfDigitsKey = FieldKeys.NO_OF_DIGITS.getName();
                final long startNumber = config.optLong(numberKey, 0);

                config.put(numberKey, startNumber);
                config.put(FieldKeys.IMMUTABLE.getName(), true);
                config.put(FieldKeys.ADD_VIEW.getName(), false);
                config.put(FieldKeys.EDITABLE_VIA_LIST.getName(), false);
                config.put(FieldTypeKeys.IS_FILTERABLE.getName(), true);
                config.put(FieldKeys.READ_ONLY.getName(), true);

                if (!config.containsKey(noOfDigitsKey)) {
                    config.put(noOfDigitsKey, 0);
                }

                if (!config.containsKey(maxKey)) {
                    config.put(maxKey, MAX_NUMBER_SIZE);
                    config.put(minKey, MIN_VALUE);
                    config.put(PresetConfig.Configuration.MIN_MESSAGE.getName(), "i18n.fields.violation.message.min");
                    config.put(PresetConfig.Configuration.MAX_MESSAGE.getName(), "i18n.fields.violation.message.max");
                }
            }
            case MONETARY -> {
                config.put(maxKey, MAX_MONETARY_VALUE);
                config.put(minKey, MIN_VALUE);
                config.put(PresetConfig.Configuration.MIN_MESSAGE.getName(), "i18n.fields.violation.message.min");
                config.put(PresetConfig.Configuration.MAX_MESSAGE.getName(), "i18n.fields.violation.message.max");
            }
            default -> {}
        }

        if (!config.containsKey(maxLengthKey)) {
            config.put(PresetConfig.Configuration.MAX_LENGTH_MESSAGE.getName(), "i18n.fields.violation.message.max.length");

            switch (storeType) {
                case SINGLE_STRING -> config.put(maxLengthKey, MAX_STRING_SIZE);
                case TEXT -> config.put(maxLengthKey, MAX_TEXT_SIZE);
            }
        }

        final Source source = Source.get(fieldSource.getName());

        switch (source) {
            case CONTACT, COMPANY, DEAL, PRODUCT -> {
                final JsonObject summaryJson = Json.object();

                summaryJson.put(FieldKeys.CATEGORY.getName(), FieldKeys.CUSTOM_FIELDS.getName());
                summaryJson.put(FieldKeys.ROW_INDEX_ON_VIEW.getName(), CUSTOM_FIELDS_SUMMARY_ROW_INDEX);

                config.put(FieldKeys.SUMMARY.getName(), summaryJson);
                config.put(FieldKeys.MOBILE_SUMMARY.getName(), summaryJson);
            }
        }

        fieldConfig.setPresetConfig(config.toString());
        field.setFieldConfig(fieldConfig);

        return field;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link Field}.
     * </p>
     *
     * @param orgId Organization id
     * @param data  A {@link JsonObject} that holds the field information
     */
    public void createField(final Long orgId, JsonObject data, final CrmResponse crmResponse) {
        final String fieldNameKey = FieldKeys.FIELD_NAME.getName();
        final String typeKey = FieldKeys.TYPE.getName();
        final String sourceIdKey = FieldKeys.SOURCE_ID.getName();
        final String name = data.containsKey(fieldNameKey) ? data.getString(fieldNameKey) : null;
        final String internalName = data.optString(FieldKeys.INTERNAL_NAME.getName(), null);
        final String type = data.containsKey(typeKey) ? data.getString(typeKey) : null;
        final Long sourceId = data.containsKey(sourceIdKey) ? data.getLong(sourceIdKey) : null;
        final FieldSource fieldSource = Objects.isNull(sourceId) ? null : fieldSourceService.getSource(sourceId);
        final StoreType storeType = Objects.isNull(type) || type.isEmpty() ? null : StoreType.valueOf(type.toUpperCase());
        final JsonObject config = data.optJsonObject(FieldKeys.CONFIG.getName());

        if (isValidDetails(name, internalName, storeType, fieldSource, config)) {
            final Field field = convertToCrmField(orgId, name, internalName, storeType, fieldSource, config);

            validateFieldNames(true, orgId, List.of(field), crmResponse);
            createFields(crmResponse, List.of(field));
        }
    }

    /**
     * <p>
     * Validates the field names for the given sources.
     * </p>
     *
     * @param isCreate Represents whether it is a create event
     * @param orgId    Represents an Organization id
     * @param fields   Represents the {@link Collection} of {@link Field}
     * @param response Represents the {@link CrmResponse}
     */
    private void validateFieldNames(final boolean isCreate, final long orgId,
                                    final Collection<Field> fields, final CrmResponse response) {
        final ErrorCode nameExistsError = ErrorCode.get(
                EntityErrorCodes.FIELD_ALREADY_EXISTS, "i18n.entity.fields.name.already.exists");

        for (final Map.Entry<String, List<Field>> sourceFields : fields.stream()
                .collect(Collectors.groupingBy(field -> field.getSource().getName())).entrySet()) {
            final Collection<FieldInfo> fieldsBySource = fieldService.getFieldsBySource(orgId, sourceFields.getKey());
            final Map<Long, String> existingNameById = fieldsBySource
                    .stream().collect(Collectors.toMap(FieldInfo::id, info ->
                            normalizeFieldName(response.resolveI18nPlaceholder(info.i18nName()))));
            final Map<Long, String> existingInternalNamesById = fieldsBySource.stream().collect(Collectors.toMap(FieldInfo::id,
                    info -> normalizeFieldName(info.getInternalName())));

            final Collection<String> existingNames = existingNameById.values();
            final Collection<String> existingInternalNames = existingInternalNamesById.values();

            for (final Field field : sourceFields.getValue()) {
                final String name = field.getName();
                final String internalName = field.getInternalName();

                if (Objects.nonNull(name)) {
                    final String normalisedName = normalizeFieldName(name);

                    if (existingNames.contains(normalisedName)) {

                        if (isCreate) {
                            throw nameExistsError;
                        } else if (!Objects.equals(existingNameById.get(field.id()), normalisedName)) {
                            throw nameExistsError;
                        }
                    }
                }

                if (Objects.nonNull(internalName)) {
                    final String fieldInternalName = normalizeFieldName(internalName);

                    if (existingInternalNames.contains(fieldInternalName)) {

                        if (isCreate) {
                            throw ErrorCode.get(EntityErrorCodes.FIELD_INTERNAL_NAME_ALREADY_EXISTS, "i18n.entity.fields.internal.name.already.exists");
                        } else if (!Objects.equals(existingInternalNamesById.get(field.id()), fieldInternalName)) {
                            throw ErrorCode.get(EntityErrorCodes.FIELD_INTERNAL_NAME_ALREADY_EXISTS, "i18n.entity.fields.internal.name.already.exists");
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Normalizes the field name by removing the empty spaces and converting to lower case.
     * </p>
     *
     * @param name Represents the field name
     * @return normalized field name
     */
    private String normalizeFieldName(final String name) {
        return name.replace(EMPTY_SPACE, StringUtil.EMPTY_STRING).toLowerCase();
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link Field}.
     * </p>
     *
     * @param orgId Organization id
     * @param data  A {@link JsonObject} that holds the field information
     */
    public void createFields(final Long orgId, JsonObject data, final CrmResponse crmResponse) {
        final Collection<Field> fields = new ArrayList<>();
        final JsonArray fieldsList = data.optJsonArray(FieldKeys.LIST.getName());
        final int size;

        if (Objects.isNull(fieldsList)) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_LIST_NOT_FOUND, "i18n.entity.field.list.not.found");
        } else {
            size = fieldsList.size();

            if (NumericalConstant.ZERO == fieldsList.size()) {
                throw ErrorCode.get(EntityErrorCodes.FIELD_LIST_NOT_FOUND, "i18n.entity.field.list.not.found");
            }
        }
        final String fieldNameKey = FieldKeys.FIELD_NAME.getName();
        final String typeKey = FieldKeys.TYPE.getName();
        final String sourceIdKey = FieldKeys.SOURCE_ID.getName();

        for (int index = 0; index < size; index++) {
            final JsonObject fieldData = fieldsList.getJsonObject(index);

            final String name = fieldData.containsKey(fieldNameKey) ? fieldData.getString(fieldNameKey) : null;
            final String internalName = data.optString(FieldKeys.INTERNAL_NAME.getName(), null);
            final String type = fieldData.containsKey(typeKey) ? fieldData.getString(typeKey) : null;
            final Long sourceId = fieldData.containsKey(sourceIdKey) ? fieldData.getLong(sourceIdKey) : null;
            final JsonObject config = fieldData.optJsonObject(FieldKeys.CONFIG.getName());
            final FieldSource fieldSource = Objects.isNull(sourceId) ? null : fieldSourceService.getSource(sourceId);
            final StoreType storeType = Objects.isNull(type) || type.isEmpty() ? null : StoreType.valueOf(type.toUpperCase());

            if (isValidDetails(name, internalName, storeType, fieldSource, config)) {
                fields.add(convertToCrmField(orgId, name, internalName, storeType, fieldSource, config));
            }
        }
        createFields(crmResponse, fields);
    }

    /**
     * <p>
     * Creates the crm entity
     * </p>
     *
     * @param crmResponse CRM response wrapper
     * @param fields      Collection of entity
     */
    public void createFields(final CrmResponse crmResponse, final Collection<Field> fields) {
        final Map<String, Object> response = secureFieldManager.createFields(fields);
        final List<String> createdFields = (List<String>) response.get(FieldKeys.SUCCESS.getName());
        final String errorKey = FieldKeys.ERROR.getName();
        final ErrorSummary errorSummary = (ErrorSummary) response.get(errorKey);

        crmResponse.put(FieldKeys.CREATED_RECORDS.getName(), Json.array(createdFields));

        if (createdFields.isEmpty() || Objects.nonNull(errorSummary)) {
            crmResponse.setErrorCode(ErrorCode.get(EntityErrorCodes.CREATE_ACTION_FAILED, "i18n.entity.fields.create.failed"));
            crmResponse.setErrorSummary(errorSummary);
        } else {
            crmResponse.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.entity.fields.create.success"));
        }
    }

    /**
     * <p>
     * Changes the order of options
     * </p>
     *
     * @param orgId      The unique identifier of the organization
     * @param jsonObject The JSON object containing the option IDs representing the new order
     * @param response   Represents the {@link CrmResponse}
     */
    private void updateOptionsOrder(final Long orgId, final JsonObject jsonObject, final CrmResponse response) {
        final long fieldId = jsonObject.getLong(FieldKeys.FIELD_ID.getName());
        final Set<Long> optionIds = new LinkedHashSet<>();

        jsonObject.getJsonArray(FieldKeys.IDS.getName()).forEach(optionId -> optionIds.add((Long) optionId));

        if (!secureFieldManager.exists(fieldId)) {
            throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, fieldNotFoundI18NKey);
        }
        final FieldInfo fieldInfo = fieldService.getField(orgId, fieldId);
        final DropdownType dropdownType = fieldInfo.presetConfig().dropdownType();

        if (DropdownType.STATIC == dropdownType || (DropdownType.HYBRID == dropdownType &&
                optionService.checkNonEditableOptionsForIds(orgId, fieldId, optionIds))) {
            throw ErrorCode.get(EntityErrorCodes.STATIC_OPTION_ORDER_MODIFICATION_RESTRICTED,
                    "i18n.entity.static.option.order.modification.restricted");
        }

        if (!optionService.exists(orgId, fieldId, optionIds, true)) {
            throw ErrorCode.get(EntityErrorCodes.OPTIONS_NOT_FOUND, "i18n.entity.fields.option.not.found");
        }

        optionManager.updateOptionsOrder(orgId, optionIds);
        response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.entity.fields.option.order.update.success"));
    }

    /**
     * <p>
     * Fetches the order of last immovable field from the provided {@link Collection} of {@link FieldInfo}
     * </p>
     *
     * @param fields Refers to the {@link Collection} of {@link FieldInfo} of a source
     * @return {@link int} order of the immovable field
     */
    private int getOrderOfLastImmovableField(final Collection<FieldInfo> fields) {
        return (int) fields.stream().filter(fieldInfo -> fieldInfo.hasPresetConfig() && fieldInfo.presetConfig().isImmovable())
                .mapToLong(fieldInfo -> fieldInfo.presetConfig().order()).reduce((first, second) -> second).orElse(0);
    }

    /**
     * <p>
     * Changes the order of fields
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject The JSON object containing the field IDs representing the new order
     * @param response   Represents the {@link CrmResponse}
     */
    private void updateFieldsOrder(final Long orgId, final JsonObject jsonObject, final CrmResponse response) {
        final Long sourceId = jsonObject.getLong(FieldKeys.SOURCE_ID.getName());
        final List<Long> fieldIds = new ArrayList<>();
        final String source = fieldSourceService.getSource(sourceId).getName();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, source);
        final int startIndexToUpdate = getOrderOfLastImmovableField(fields) + 1;
        final Map<Integer, Long> dependableFieldMap = new TreeMap<>();
        final Map<Long, FieldInfo> fieldMap = fields.stream().collect(Collectors.toMap(FieldInfo::id, Function.identity()));
        final Map<Integer, Map<Long, FieldInfo>> groupedFields = fields.stream().collect(
                Collectors.groupingBy(fieldInfo -> fieldInfo.presetConfig().getGroupId(), Collectors.toMap(FieldInfo::id, Function.identity())));
        final Counter fieldIdCounter = new Counter();

        jsonObject.getJsonArray(FieldKeys.IDS.getName()).forEach(fieldId -> fieldIds.add((Long) fieldId));

        for (final Long fieldId : fieldIds) {
            final FieldInfo field = fieldMap.get(fieldId);

            if (field.presetConfig().isImmovable()) {
                throw ErrorCode.get(EntityErrorCodes.IMMOVABLE_FIELDS_ORDER_CAN_NOT_BE_CHANGED);
            }
            final int groupId = field.presetConfig().getGroupId();

            if (groupId > 0) {
                if (groupedFields.containsKey(groupId)) {
                    final Map<Long, FieldInfo> dependableFields = groupedFields.remove(groupId);
                    final int startingIndex = fieldIds.indexOf(fieldId);
                    final ListIterator<Long> fieldIterator = fieldIds.listIterator(startingIndex);

                    fieldIdCounter.setCount(startingIndex);

                    while (fieldIterator.hasNext()) {
                        final FieldInfo dependableField = dependableFields.remove(fieldIterator.next());

                        if (Objects.isNull(dependableField)) {

                            if (dependableFields.isEmpty() || dependableFields.values().stream().allMatch(fieldInfo -> fieldInfo.presetConfig().isImmovable())) {
                                break;
                            } else {
                                throw ErrorCode.get(EntityErrorCodes.COULD_NOT_BE_CHANGED_DEPENDABLE_FIELD_ORDER);
                            }
                        } else {
                            dependableFieldMap.put(dependableField.presetConfig().order(), dependableField.id());
                        }
                    }
                }
            }
        }

        for (final Map.Entry<Integer, Long> entry : dependableFieldMap.entrySet()) {
            fieldIds.add(fieldIdCounter.getAndIncrement(), entry.getValue());
        }

        fieldManager.updateFieldsOrder(orgId, new LinkedHashSet<>(fieldIds), startIndexToUpdate);
        response.put(FieldKeys.MESSAGE.getName(), response.resolveI18nString("i18n.entity.fields.update.success"));
    }

    /**
     * <p>
     * Validates the details given with preset config values.
     * </p>
     *
     * @param field         Refers {@link Field}.
     * @param currentConfig Holds details of request config data.
     * @param presetConfig  Holds field's preset config.
     * @param jsonField     Holds details of the data.
     * @param crmResponse   Refers {@link CrmResponse}.
     * @return whether the details are valid.
     */
    private boolean isValidDetails(final Field field, final JsonObject currentConfig,
                                   final PresetConfig presetConfig, final JsonObject jsonField, final CrmResponse crmResponse) {
        final boolean isPersistent = field.isPersistent();
        final boolean isReadable = presetConfig.readOnly();
        final String readOnlyKey = FieldKeys.READ_ONLY.getName();
        final String optionsKey = FieldKeys.OPTIONS.getName();
        final String typeKey = FieldKeys.TYPE.getName();
        final String isHiddenKey = FieldKeys.IS_HIDDEN.getName();
        final String dropdownTypeKey = FieldKeys.DROPDOWN_TYPE.getName();
        final String fieldNameKey = FieldKeys.FIELD_NAME.getName();
        final String sourceIdKey = FieldKeys.SOURCE_ID.getName();
        final String addViewKey = FieldKeys.ADD_VIEW.getName();
        final String requiredKey = FieldKeys.REQUIRED.getName();
        final DropdownType dropdownType = presetConfig.dropdownType();
        final String editableViaListKey = FieldKeys.EDITABLE_VIA_LIST.getName();
        final StoreType storeType = field.getFieldType().storeType();

        if (jsonField.containsKey(typeKey) &&
                !Objects.equals(storeType, StoreType.valueOf(jsonField.getString(typeKey)))) {
            throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.fields.type.modification.restricted");
        }

        if (currentConfig.containsKey(dropdownTypeKey)
                && !Objects.equals(presetConfig.dropdownTypeName(), currentConfig.getString(dropdownTypeKey))) {
            throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.fields.dropdown.type.modification.restricted");
        }

        if (currentConfig.containsKey(editableViaListKey)) {
            if (!Objects.equals(currentConfig.getBoolean(editableViaListKey), presetConfig.isEditableViaList())) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.fields.editable.via.list.key.modification.restricted");
            }
        }

        if (isPersistent) {

            if (currentConfig.containsKey(FieldKeys.IMMUTABLE.getName())) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.immutable.config.access.restricted");
            }

            if (currentConfig.optBoolean(readOnlyKey)) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.config.readable.add.restricted");
            }

            if (currentConfig.containsKey(isHiddenKey)) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.config.hidden.access.restricted");
            }

            if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
                validateOptions(currentConfig.optJsonArray(FieldKeys.OPTIONS.getName()), true);
            }
        } else {
            if (presetConfig.readOnly() && !currentConfig.optBoolean(readOnlyKey, true)) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.config.readable.modification.restricted");
            }

            if ((currentConfig.optBoolean(addViewKey) || currentConfig.optBoolean(requiredKey)) && isReadable) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.config.readable.required.modification.restricted");
            }

            if (presetConfig.immutable()) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED,"i18n.entity.field.immutable.field.modification.restricted");
            }

            if (currentConfig.optBoolean(isHiddenKey) && (!presetConfig.isFieldEnabled() || presetConfig.isDefaultSystemField())) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.field.config.hidden.access.restricted");
            }

            if (jsonField.containsKey(fieldNameKey) && !Objects.equals(
                    crmResponse.resolveI18nPlaceholder(field.getI18nName()), jsonField.opt(fieldNameKey))) {
                throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.default.fieldName.modification.restricted");
            }

            if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
                validateOptions(currentConfig.optJsonArray(FieldKeys.OPTIONS.getName()), false);
            }

            if (Objects.nonNull(dropdownType)) {

                if (dropdownType == DropdownType.STATIC && currentConfig.containsKey(optionsKey)) {
                    final JsonArray jsonArray = currentConfig.getJsonArray(optionsKey);

                    for (int index = 0; index < jsonArray.size(); index++) {

                        if (Objects.isNull(jsonArray.getJsonObject(index).opt(FieldKeys.VALUE.getName()))) {
                            throw ErrorCode.get(EntityErrorCodes.STATIC_OPTION_CREATION_RESTRICTED, "i18n.entity.static.option.creation.restricted");
                        }
                    }
                }
            }
            final Long sourceId = field.getSource().id();

            if (!Objects.equals(sourceId, jsonField.optLong(sourceIdKey, sourceId))) {
                throw ErrorCode.get(EntityErrorCodes.SOURCE_MODIFICATION_RESTRICTED, "i18n.entity.source.modification.restricted");
            }

            final String emailsKey = FieldKeys.EMAILS.getName();
            final String phonesKey = FieldKeys.PHONES.getName();
            final Map<String, FieldInfo> contactFields = fieldService.groupDefaultFieldsByAliasName(field.orgId(), Source.CONTACT.getName(), List.of(emailsKey, phonesKey));
            final FieldInfo emailFieldInfo = contactFields.get(emailsKey);
            final FieldInfo phoneFieldInfo = contactFields.get(phonesKey);

            if (Objects.equals(emailFieldInfo.sourceId(), jsonField.optLong(sourceIdKey, field.getSource().id()))) {
                final Long fieldId = field.id();
                final PresetConfig phonePresetConfig = phoneFieldInfo.presetConfig();
                final PresetConfig emailPresetConfig = emailFieldInfo.presetConfig();
                final boolean isEmailField = Objects.equals(fieldId, emailFieldInfo.id());
                final boolean isPhoneField = Objects.equals(fieldId, phoneFieldInfo.id());

                if (currentConfig.containsKey(requiredKey)) {
                    if (!currentConfig.getBoolean(requiredKey) && (isEmailField && !phonePresetConfig.required() || isPhoneField && !emailPresetConfig.required())) {
                        throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.field.config.required.type.modification.restricted");
                    }
                }

                if (currentConfig.containsKey(addViewKey)) {
                    if (!currentConfig.getBoolean(addViewKey) && (isEmailField && !phonePresetConfig.addView()) || isPhoneField && !emailPresetConfig.addView()) {
                        throw ErrorCode.get(EntityErrorCodes.CRM_FIELD_TYPE_MODIFICATION_IS_RESTRICTED, "i18n.entity.field.config.addView.type.modification.restricted");
                    }
                }
            }
        }

        return true;
    }

    /**
     * <p>
     * Checks whether the data is valid.
     * </p>
     *
     * @param fieldName    Field's name.
     * @param internalName Internal name of the field.
     * @param storeType    Field's type.
     * @param fieldSource  {@link FieldSource}.
     * @param config       Field's config.
     * @return whether the data is valid
     */
    private boolean isValidDetails(final String fieldName, final String internalName, final StoreType storeType, final FieldSource fieldSource, final JsonObject config) {

        if (Objects.isNull(fieldName) || fieldName.isEmpty()) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_NAME_NOT_FOUND, "i18n.entity.field.name.not.found");
        }

        if (Objects.isNull(internalName) || internalName.isEmpty() || !internalName.matches(INTERNAL_NAME_REGEX)) {
            throw ErrorCode.get(EntityErrorCodes.INVALID_FIELD_INTERNAL_NAME, "i18n.entity.field.internal.name.invalid");
        }

        if (Objects.isNull(storeType)) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_TYPE_NOT_FOUND, "i18n.entity.field.type.not.found");
        } else if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
            validateOptions(config.optJsonArray(FieldKeys.OPTIONS.getName()), true);
        }

        if (Objects.isNull(config)) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_CONFIG_NOT_FOUND, "i18n.entity.field.config.not.found");
        }

        if (config.optBoolean(FieldKeys.IMMUTABLE.getName())) {
            throw ErrorCode.get(EntityErrorCodes.CUSTOM_FIELDS_CANNOT_BE_IMMUTABLE, "i18n.entity.field.immutable.config.access.restricted");
        }

        if (Objects.isNull(fieldSource)) {
            throw ErrorCode.get(EntityErrorCodes.FIELD_SOURCE_NOT_FOUND, "i18n.entity.field.source.not.found");
        }

        if (config.containsKey(FieldKeys.IS_HIDDEN.getName())) {
            throw ErrorCode.get(EntityErrorCodes.CUSTOM_FIELDS_CANNOT_BE_HIDDEN, "i18n.entity.fields.cannot.be.hidden");
        }

        if (!fieldSource.isCommon()) {
            throw ErrorCode.get(EntityErrorCodes.CUSTOM_FIELD_CREATION_RESTRICTED, "i18n.entity.fields.custom.field.creation.restricted");
        }
        return true;
    }

    /**
     * <p>
     * Validates that the provided list of options meets the allowed constraints.
     * </p>
     *
     * @param options      The JSON array containing the options to validate.
     * @param isPersistent Indicates whether the field is persistent or not, which affects the validation rules.
     */
    private void validateOptions(final JsonArray options, final boolean isPersistent) {
        final ErrorCode minimumCountError = ErrorCode.get(
                EntityErrorCodes.MINIMUM_TWO_OPTION_REQUIRED, "i18n.entity.fields.minimum.option.required"
        );

        if (Objects.nonNull(options)) {
            final int optionSize = options.size();

            if (isPersistent) {

                if (NumericalConstant.TWO > optionSize) {
                    throw minimumCountError;
                } else if (NumericalConstant.ONE_HUNDRED < optionSize) {
                    throw ErrorCode.get(
                            EntityErrorCodes.MAXIMUM_OPTION_LIMIT_REACHED, "i18n.entity.fields.maximum.option.limit.reached"
                    );
                }
            }
            final String optionNameKey = FieldKeys.NAME.getName();

            for (int index = NumericalConstant.ZERO; index < optionSize; index++) {
                final JsonObject optionJson = options.getJsonObject(index);

                if (Objects.isNull(optionJson.getString(optionNameKey))) {
                    throw ErrorCode.get(
                            EntityErrorCodes.DROPDOWN_OPTIONS_REQUIRED, "i18n.entity.field.type.dropdown.option.notEmpty"
                    );
                }
            }
        } else if (isPersistent) {
            throw minimumCountError;
        }
    }

    /**
     * <p>
     * Modifies the option names based on dropdown type.
     * </p>
     *
     * @param orgId         Represents an organization's id.
     * @param fieldId       A field's id.
     * @param dropdownType  Refers {@link DropdownType}.
     * @param currentConfig Holds details of request config data.
     */
    private void modifyOptions(final Long orgId, final Long fieldId, final DropdownType dropdownType, final JsonObject currentConfig) {
        final String optionsKey = FieldKeys.OPTIONS.getName();

        if ((currentConfig.containsKey(optionsKey)) && (DropdownType.STATIC == dropdownType || DropdownType.HYBRID == dropdownType)) {
            final String valueKey = FieldKeys.VALUE.getName();
            final JsonArray jsonArray = currentConfig.getJsonArray(optionsKey);
            final Collection<Option> options = optionService.get(orgId, fieldService.getField(orgId, fieldId));
            final String nameKey = OptionKeys.NAME;
            final String editableOptionKey = OptionKeys.IS_EDITABLE_OPTION;
            final Map<Long, Option> optionValues = options.stream().collect(Collectors.toMap(Option::id, Function.identity()));
            final int size = jsonArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject jsonObject = jsonArray.getJsonObject(index);

                if (!jsonObject.optBoolean(editableOptionKey)) {
                    final long optionId = jsonObject.optLong(valueKey, 0);

                    if (optionId > 0) {
                        final Option option = optionValues.get(optionId);

                        if (Objects.nonNull(option)) {
                            jsonObject.put(nameKey, option.getOption());
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Converts the {@link Collection} of {@link FieldInfo} to {@link JsonArray}
     * </p>
     *
     * @param fields                Represents the {@link Collection of {@link FieldInfo}
     * @param isSourceActivity      Represent whether the source is activity or not
     * @param crmResponse           Represents the {@link CrmResponse}
     * @param i18nResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @return {@link JsonArray}
     */
    private JsonArray convertFieldsIntoArray(final boolean isPrimarySource, final Collection<FieldInfo> fields,
                                             final CrmResponse crmResponse, final I18nResolutionAdapter i18nResolutionAdapter) {
        final JsonArray jsonArray = Json.array();
        final String idKey = FieldKeys.ID.getName();
        final String fieldNameKey = FieldKeys.FIELD_NAME.getName();
        final String typeKey = FieldKeys.TYPE.getName();
        final String patternKey = FieldKeys.PATTERN.getName();
        final String violationMessageKey = FieldKeys.VIOLATION_MESSAGE.getName();
        final String placeHolderKey = FieldKeys.PLACE_HOLDER.getName();
        final String configKey = FieldKeys.CONFIG.getName();
        final String internalNameKey = FieldKeys.INTERNAL_NAME.getName();

        for (final FieldInfo fieldInfo : fields) {
            final PresetConfig presetConfig = fieldInfo.presetConfig();
            final JsonObject presetConfigJson = presetConfig.presetConfigAsJson();
            final I18nKey placeHolder = fieldInfo.getPlaceHolder();
            final JsonObject jsonObject = Json.object();

            jsonObject.put(idKey, isPrimarySource ? fieldInfo.id() : fieldInfo.aliasName());
            jsonObject.put(fieldNameKey, crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
            jsonObject.put(internalNameKey, crmResponse.resolveI18nString(fieldInfo.getInternalName()));
            jsonObject.put(FieldKeys.ALIAS_NAME.getName(), fieldInfo.aliasName());
            jsonObject.put(typeKey, fieldInfo.storeType());

            if (presetConfigJson.containsKey(patternKey)) {
                final JsonObject validationJson = presetConfigJson.getJsonObject(patternKey);

                if (validationJson.containsKey(violationMessageKey)) {
                    final String violationMessage = validationJson.getString(violationMessageKey);

                    validationJson.put(FieldKeys.MESSAGE.getName(), crmResponse.resolveI18nPlaceholder(violationMessage));
                }
            }

            if (Objects.nonNull(placeHolder)) {
                jsonObject.put(placeHolderKey, crmResponse.resolveI18nPlaceholder(placeHolder.getKey()));
                presetConfigJson.remove(placeHolderKey);
            }
            fieldInfo.presetConfig().getMaxValue().ifPresent(value -> presetConfigJson.put(FieldKeys.MAX_DATE.getName(), value));
            final JsonObject validationJson = fieldConstraintBuilder.build(fieldInfo, presetConfigJson, i18nResolutionAdapter);

            if (0 < validationJson.size()) {
                presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
            }

            jsonObject.put(configKey, presetConfigJson);
            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /**
     * <p>
     * Converts the field info into {@link JsonObject}
     * </p>
     *
     * @param isPrimarySource       Represents whether the source is registered
     * @param fieldInfo             Represents the {@link FieldInfo}
     * @param crmResponse           Represents the {@link CrmResponse}
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}.
     * @return The {@link JsonObject}
     */
    private JsonObject convertFieldInfoIntoJsonObject(final boolean isPrimarySource, final FieldInfo fieldInfo, final CrmResponse crmResponse,
                                                      final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonObject presetConfigJson = fieldInfo.presetConfig().presetConfigAsJson();
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.ID.getName(), isPrimarySource ? fieldInfo.id() : fieldInfo.aliasName());
        jsonObject.put(FieldKeys.FIELD_NAME.getName(), crmResponse.resolveI18nPlaceholder(fieldInfo.i18nName()));
        jsonObject.put(FieldKeys.ALIAS_NAME.getName(), fieldInfo.aliasName());
        jsonObject.put(FieldKeys.TYPE.getName(), fieldInfo.storeType());

        final JsonObject validationJson = fieldConstraintBuilder.build(fieldInfo, presetConfigJson, i18NResolutionAdapter);

        if (0 < validationJson.size()) {
            presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
        }

        return jsonObject.put(FieldKeys.CONFIG.getName(), presetConfigJson);
    }

    /**
     * <p>
     * Process the form rendering fields for an given organization ID based on the required configurations
     * </p>
     *
     * @param orgId                       Represents an organization id
     * @param includeDependableFieldGroup Represents whether the dependent fields need to be grouped into a single array or not
     * @param jsonObject                  Represents the {@link JsonObject}
     * @param crmRequest                  Represents the {@link CrmRequest}
     * @param crmResponse                 Represents the {@link CrmResponse}
     */
    private void dispatchFormRenderFields(final long orgId, final boolean includeDependableFieldGroup, final JsonObject jsonObject, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final String source = jsonObject.getString(FieldKeys.SOURCE.getName());
        final String viewKey = FieldKeys.VIEW.getName();
        final String addViewKey = FieldKeys.ADD_VIEW.getName();
        final String profileEditViewKey = FieldKeys.PROFILE_EDIT_VIEW.getName();
        final String view = jsonObject.containsKey(viewKey) ? jsonObject.getString(viewKey) : addViewKey;
        final boolean isAddView = Objects.equals(addViewKey, view);
        final boolean isProfileEditView = Objects.equals(profileEditViewKey, view);
        final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(orgId, source);

        if (fieldSource.getSettings().isEnabled()) {
            final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, source);

            if (fields.isEmpty()) {
                throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, fieldNotFoundI18NKey);
            } else {
                final Collection<FieldVisibilityRule> fieldVisibilityRules = new ArrayList<>();
                final FieldVisibilityHandler fieldVisibilityHandler = FieldVisibilityHandler.get(Source.get(source));
                final boolean isVisibilityHandlerNull = Objects.isNull(fieldVisibilityHandler);

                for (final FieldInfo fieldInfo : fields) {
                    final Optional<FieldVisibilityCriteriaGroup> fieldVisibilityCriteriaGroup = isVisibilityHandlerNull ||
                            fieldInfo.isPersistent() ? Optional.empty() : fieldVisibilityHandler.getCriteriaGroup(orgId, fieldInfo);
                    final FieldVisibilityRule fieldVisibilityRule = fieldVisibilityCriteriaGroup.map(criteriaGroup ->
                                    fieldVisibilityRuleService.getFieldVisibilityRule(fieldInfo, criteriaGroup))
                            .orElseGet(() -> fieldVisibilityRuleService.getFieldVisibilityRule(fieldInfo, new FieldVisibilityCriteriaGroup()));

                    fieldVisibilityRules.add(fieldVisibilityRule);
                }
                final Predicate<FieldVisibilityRule> visibilityRules = getFieldVisibilityRulePredicate(view, isAddView);
                final Collection<FieldVisibilityRule> configuredFields = getConfiguredFieldVisibilityRules(orgId, fieldSource, fieldVisibilityRules).stream().filter(visibilityRules).toList();

                if (!configuredFields.isEmpty()) {
                    final JsonObject fieldSubTypeJson = getFieldSubTypeByGroup(configuredFields, isProfileEditView, crmRequest.i18nResolver(), crmResponse, isAddView);

                    crmResponse.put(FieldKeys.SOURCE.getName(), convertConfiguredSourceToJson(fieldSource));
                    crmResponse.put(FieldKeys.FIELDS.getName(), convertFieldsInfoInOrder(orgId, crmRequest.getI18NCRMResolutionAdapter(),
                            configuredFields, isAddView, isProfileEditView, includeDependableFieldGroup));

                    if (fieldSubTypeJson.size() > 0) {
                        crmResponse.put(FieldKeys.SUB_FIELDS.getName(), fieldSubTypeJson);
                    }
                }
            }
        } else {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_SOURCE_IS_DISABLED,
                    String.format(crmResponse.resolveI18nString("i18n.entity.source.disable"), crmResponse.resolveI18nString("i18n.entity.source.product")));
        }
    }

    /**
     * <p>
     * Gets the field visibility rule predicate
     * </p>
     *
     * @param view      Represents the view type
     * @param isAddView Represents the add view
     * @return The {@link FieldVisibilityRule} predicate
     */
    private Predicate<FieldVisibilityRule> getFieldVisibilityRulePredicate(final String view, final boolean isAddView) {
        return visibilityRule -> {

            switch (view) {
                case "profileEditView" -> {
                    final PresetConfig presetConfig = visibilityRule.fieldInfo().presetConfig();

                    return presetConfig.showOnProfile() && !presetConfig.readOnly();
                }
                case "updateView" -> {
                    final PresetConfig presetConfig = visibilityRule.fieldInfo().presetConfig();

                    return !presetConfig.readOnly();
                }
                default -> {
                    final PresetConfig presetConfig = visibilityRule.fieldInfo().presetConfig();

                    return isAddView ? presetConfig.addView() : !presetConfig.readOnly();
                }
            }
        };
    }

    /**
     * <p>
     * Updates the summary field preference for a user and source
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param data        Represents the {@link JsonObject} containing preference data
     */
    private void updateSummaryFieldPreference(final Long orgId, final Long userId, final JsonObject data) {
        try {
            final Source source = Source.get(data.getString(FieldKeys.SOURCE.getName()));

            if (Source.isPrimarySource(source)) {
                final JsonObject preferenceJson = data.getJsonObject(FieldKeys.FIELD_PREFERENCE.getName());
                final SummaryFieldPreference preference = new SummaryFieldPreference();

                preference.setOrgId(orgId);
                preference.setUserId(userId);
                preference.setSourceId(source.getId());
                preference.setPreferenceAsJson(preferenceJson);

                summaryFieldPreferenceManager.update(orgId, preference);
            } else {
                throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE, "i18n.source.invalid");
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EntityErrorCodes.UPDATE_ACTION_FAILED, "i18n.entity.fields.summary.preference.update.failed");
        }
    }
}

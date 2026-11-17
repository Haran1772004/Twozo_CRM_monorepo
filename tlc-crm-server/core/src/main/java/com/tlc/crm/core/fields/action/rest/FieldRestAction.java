package com.tlc.crm.core.fields.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;

import com.tlc.crm.core.api.field.manager.DependableFieldsDataManager;
import com.tlc.crm.core.api.product.manager.ProductManager;
import com.tlc.crm.core.fields.api.model.FieldVisibilityRule;
import com.tlc.crm.core.fields.service.FieldVisibilityRuleService;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.internal.DropdownActionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.FieldTypeKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.manager.CrmFieldsManager;
import com.tlc.crm.entity.manager.field.type.OptionManager;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.converter.FieldVisibilityGroupConverter;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.service.OptionService;

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

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * <p>
 * Manages {@link Field} related REST web actions
 * </p>
 *
 * @author ThalaimalaiPanidyanT
 * @version 1.0
 */
@RestApiHandler(path = "/field")
public class FieldRestAction extends AbstractSecureRestAction {

    private final FieldService fieldService;
    private final CrmFieldsManager fieldManager;
    private final DropdownActionContainer dropdownActionContainer;
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

    public FieldRestAction() {
        this.dropdownActionContainer = DropdownActionContainer.getInstance();
        this.fieldService = FieldService.getInstance();
        this.fieldManager = CrmFieldsManager.getFieldsManager();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.fieldStoreService = FieldStoreService.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.optionService = OptionService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.optionManager = OptionManager.getInstance();
        this.fieldVisibilityGroupConverter = FieldVisibilityGroupConverter.getInstance();
        this.fieldVisibilityRuleService = FieldVisibilityRuleService.getInstance();
        this.productManager = ProductManager.getInstance();
        this.dependableFieldsDataManager = DependableFieldsDataManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(method = HttpMethod.GET)
    public void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final boolean needGroupView = Boolean.getBoolean(request.getParameter("needGroupView"));
        final String entitySource = Source.get(request.getParameter("source")).getName();

        final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(orgId, entitySource);

        if (fieldSource.isCommon()) {
            if (fieldSource.getSettings().isEnabled()) {
                final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, entitySource);

                if (fields.isEmpty()) {
                    throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, "i18n.entity.fields.not.found");
                } else {
                    response.put(FieldKeys.FIELDS.getName(), convertFieldsInfoToJsonArray(orgId, request.getI18NCRMResolutionAdapter(), fields, needGroupView));
                }
            }
            response.put(FieldKeys.SOURCE.getName(), convertConfiguredSourceToJson(fieldSource));
        } else {
            throw ErrorCode.get(EntityErrorCodes.ENTITY_TYPE_NOT_FOUND, "i18n.entity.type.not.found");
        }

    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/fieldTypes", method = HttpMethod.GET)
    public void getAllFieldTypes(final CrmRequest request, final CrmResponse response) {
        response.put(FieldKeys.LIST.getName(), getFieldTypes(response));
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/dropdown", method = HttpMethod.GET)
    public void dropdown(final CrmRequest request, final CrmResponse response) {
        final Optional<String> optionalIdParam = request.getParam("id").findFirst();
        final Optional<String> optionalNameParam = request.getParam("name").findFirst();

        if (optionalIdParam.isPresent()) {
            final DropdownRequest dropdownRequest = new DropdownRequest(request);
            final Long fieldId = Long.parseLong(optionalIdParam.get());
            final FieldInfo field = fieldService.getField(dropdownRequest.getOrgId(), fieldId);
            final StoreType storeType = field.storeType();

            if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
                final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(field);

                loadOptions(dropdownRequest, field, dropdownAction, response);
            } else {
                throw ErrorCode.get(EntityErrorCodes.FIELD_IS_NOT_A_DROPDOWN, "i18n.entity.field.not.dropdown");
            }
        } else if (optionalNameParam.isPresent()) {
            final DropdownRequest dropdownRequest = new DropdownRequest(request);
            final String name = optionalNameParam.get();
            final AsyncDropdownAction asyncDropdownAction = dropdownActionContainer.fetchAsyncAction(name);

            if (Objects.isNull(asyncDropdownAction)) {
                final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(name);

                loadOptions(dropdownRequest, null, dropdownAction, response);
            } else {
                final String pageParam = request.getParameter("page");
                final String sizeParam = request.getParameter("size");
                final int page = Objects.isNull(pageParam) ? NumericalConstant.ONE : Integer.parseInt(pageParam);
                final int limit = Objects.isNull(sizeParam) ? NumericalConstant.ONE_HUNDRED : Integer.parseInt(sizeParam);
                final int start = ((page - 1) * limit) + 1;
                final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
                final DropdownCriteria dropdownCriteria = new DropdownCriteria();
                final Object criteriaValue = request.getParameter("value");
                final String criteriaType = request.getParameter("type");

                if (Objects.nonNull(criteriaValue) && Objects.nonNull(criteriaType)) {
                    dropdownCriteria.setCriteriaValue(criteriaValue);
                    dropdownCriteria.setCriteriaType(criteriaType);
                }
                dropdownRequest.setLimit(dropdownLimit);
                dropdownRequest.setCriteria(dropdownCriteria);
                asyncDropdownAction.fetchAndDispatchData(dropdownRequest, response);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the payload of the resource to be created.
     * @param response The CRM response used to send the created resource or creation status.
     */
    public void post(final CrmRequest request, final CrmResponse response) {

    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing the partial update payload.
     * @param response The CRM response used to return the modified resource or status details.
     */
    public void patch(final CrmRequest request, final CrmResponse response) {

    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing identifiers or parameters required to locate the resource.
     * @param response The CRM response used to send deletion confirmation or error details.
     */
    public void delete(final CrmRequest request, final CrmResponse response) {

    }

    /**
     * Converts the fields to the JsonArray.
     *
     * @param orgId                 The organization ID.
     * @param i18NResolutionAdapter The i18n adapter to sent response.
     * @param fields                The collection of fields.
     */
    private JsonArray convertFieldsInfoToJsonArray(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final Collection<FieldInfo> fields,
                                                   final boolean isNeedGroupView) {
        final JsonArray jsonArray = Json.array();
        final JsonArray dependableJson = Json.array();

        if (isNeedGroupView) {
            int dependableGroupKey = 0;

            for (final FieldInfo fieldInfo : fields) {
                final int groupId = fieldInfo.presetConfig().getGroupId();
                final JsonObject fieldInfoJson = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(fieldInfo), fieldInfo.storeType());

                if (groupId != dependableGroupKey) {
                    if (dependableJson.size() > 0) {
                        final JsonArray dependableJsonArray = Json.array(dependableJson.toString());

                        jsonArray.put(dependableJsonArray);
                        dependableJson.clear();
                    }
                }

                if (groupId == 0) {
                    final JsonArray fieldJsonArray = Json.array();

                    fieldJsonArray.put(fieldInfoJson);
                    jsonArray.put(fieldJsonArray);
                } else {
                    dependableJson.put(fieldInfoJson);
                }
                dependableGroupKey = groupId;
            }
        } else {
            fields.forEach(fieldInfo -> {
                final JsonObject jsonObject = convertFieldInfoToJsonObject(orgId, i18NResolutionAdapter, new FieldVisibilityRule(fieldInfo), fieldInfo.storeType());

                jsonArray.put(jsonObject);
            });
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link FieldInfo} to {@link JsonObject}
     * </p>
     *
     * @param orgId                 Represents the organization id
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @param fieldVisibilityRule   Represents the {@link FieldVisibilityRule}
     * @param storeType             Represents the {@link StoreType}
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldInfoToJsonObject(final long orgId, final I18nResolutionAdapter i18NResolutionAdapter, final FieldVisibilityRule fieldVisibilityRule, final StoreType storeType) {
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

        final String patternKey = FieldKeys.PATTERN.getName();

        if (presetConfigJson.containsKey(patternKey)) {
            final JsonObject validationJson = presetConfigJson.getJsonObject(patternKey);
            final String violationErrorKey = FieldKeys.VIOLATION_MESSAGE.getName();

            if (validationJson.containsKey(violationErrorKey)) {
                final String violationError = validationJson.getString(violationErrorKey);

                validationJson.put(violationErrorKey, i18NResolutionAdapter.get(violationError));
            }
            presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
        }

        if (presetConfigJson.containsKey(referrerFieldsKey)) {
            final Collection<String> fieldNames = new ArrayList<>();

            presetConfigJson.getJsonArray(referrerFieldsKey).forEach(fieldName -> fieldNames.add((String) fieldName));
            final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(field.fieldSource().getName(), fieldNames).values();
            final JsonArray referrerFieldsArray = Json.array();
            final String fieldsKey = FieldKeys.ID.getName();
            final String fieldTypeKey = FieldKeys.FIELD_TYPE.getName();

            for (final FieldInfo fieldInfo : fields) {
                referrerFieldsArray.put(Json.object().put(fieldsKey, fieldInfo.id()).put(fieldTypeKey, fieldInfo.storeType()));
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
        jsonObject.put(FieldKeys.FIELD_NAME.getName(), i18NResolutionAdapter.get(field.i18nName()));
        jsonObject.put(FieldKeys.IS_ACTIVE.getName(), field.isActive());
        jsonObject.put(FieldKeys.CONFIG.getName(), presetConfigJson);
        jsonObject.put(FieldKeys.ID.getName(), field.id());
        jsonObject.put(FieldKeys.ORG_ID.getName(), field.orgId());
        final JsonArray subfields = buildSubFieldsJson(field, false, i18NResolutionAdapter, false);

        if (Objects.nonNull(subfields)) {
            jsonObject.put(FieldKeys.SUB_FIELDS.getName(), subfields);
        }
        if (Objects.nonNull(placeHolder)) {
            jsonObject.put(FieldTypeKeys.PLACEHOLDER.getName(), i18NResolutionAdapter.get(placeHolder.getKey()));
        }
        presetConfig.getMaxValue().ifPresent(value -> presetConfigJson.put(FieldKeys.MAX_DATE.getName(), value));

        switch (storeType) {
            case CONTACT_ONBOARD, CONTACTS_ONBOARD -> relatedFieldConsumer.accept(Source.CONTACT.getName(),
                    FieldKeys.FIRST_NAME.getName());
            case COMPANY_ONBOARD -> relatedFieldConsumer.accept(Source.COMPANY.getName(), FieldKeys.NAME.getName());
            case DEAL_VALUE -> relatedFieldConsumer.accept(Source.PRODUCT.getName(), FieldKeys.NAME.getName());
        }

        return jsonObject;
    }

    /**
     * Builds the sub-fields json for the given field.
     *
     * @param field                 Refers the field.
     * @param isProfileEditView     Refers the profile edit view or not.
     * @param i18NResolutionAdapter The {@code I18nResolutionAdapter}.
     * @param isAddView             Refers the add view or not.
     * @return The sub-fields array.
     */
    private JsonArray buildSubFieldsJson(final FieldInfo field, final boolean isProfileEditView, final I18nResolutionAdapter i18NResolutionAdapter, final boolean isAddView) {
        final StoreType storeType = field.storeType();

        if (storeType.hasSubtypes() || storeType.hasSourceDependentFieldSubTypes()) {
            final boolean isMonetaryType = storeType.isMonetaryType();
            final JsonArray jsonArray = Json.array();

            for (final FieldSubType fieldSubType : fieldTypeService.getSubTypesByStoreType(storeType)) {

                if (isProfileEditView && isMonetaryType && StoreSubType.PRODUCT == fieldSubType.storeSubType()) {
                    continue;
                }

                final JsonObject subTypeJson = Json.object();
                final String config = fieldSubType.config();

                subTypeJson.put(FieldTypeKeys.ID.getName(), fieldSubType.id());
                subTypeJson.put(FieldTypeKeys.NAME.getName(), fieldSubType.name());
                subTypeJson.put(FieldTypeKeys.TYPE.getName(), fieldSubType.fieldType().storeType().getName());
                subTypeJson.put(FieldTypeKeys.DISPLAY_NAME.getName(), i18NResolutionAdapter.get((fieldSubType.displayName())));

                if (Objects.nonNull(config)) {
                    subTypeJson.put(FieldTypeKeys.CONFIG.getName(), Json.object(config));
                }
                subTypeJson.put(FieldTypeKeys.INPUT_TYPE.getName(), fieldSubType.inputType());
                subTypeJson.put(FieldTypeKeys.PLACEHOLDER.getName(), i18NResolutionAdapter.get(fieldSubType.placeholder()));

                if (!isAddView) {
                    final JsonObject configJson = subTypeJson.optJsonObject(FieldTypeKeys.CONFIG.getName());

                    if (Objects.nonNull(configJson)) {
                        configJson.remove(PresetConfig.Configuration.IS_DUPLICATE_CHECK_FIELD.getName());
                    }
                }
                jsonArray.put(subTypeJson);
            }
            return jsonArray;

        }
        return null;
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
     * Gets the collection of {@link FieldType}
     *
     * @param response crm response
     * @return {@link Collection} of {@link FieldType}
     */
    private JsonArray getFieldTypes(final CrmResponse response) {
        final JsonArray fieldTypeArray = Json.array();
        final Collection<FieldType> fieldTypes = fieldTypeService.getAllFieldTypes();

        if (Objects.nonNull(fieldTypes)) {
            fieldTypes.forEach(type -> fieldTypeArray.put(convertFieldTypeIntoJson(type,
                    fieldTypeService.getSubTypesByFieldTypeId(type.id()), response)));
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
                                                final CrmResponse response) {
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

            fieldSubTypes.forEach(subType -> fieldSubTypeArray.put(convertFieldSubTypeIntoJson(subType, response)));

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
    private JsonObject convertFieldSubTypeIntoJson(final FieldSubType subType, final CrmResponse response) {
        final JsonObject fieldSubTypeJson = Json.object();
        final String config = subType.config();

        fieldSubTypeJson.put(FieldTypeKeys.ID.getName(), subType.id());
        fieldSubTypeJson.put(FieldTypeKeys.NAME.getName(), subType.name());
        fieldSubTypeJson.put(FieldTypeKeys.TYPE.getName(), subType.fieldType().storeType().getName());
        fieldSubTypeJson.put(FieldTypeKeys.DISPLAY_NAME.getName(), response.resolveI18nPlaceholder(subType.displayName()));

        if (Objects.nonNull(config)) {
            fieldSubTypeJson.put(FieldTypeKeys.CONFIG.getName(), Json.object(config));
        }
        fieldSubTypeJson.put(FieldTypeKeys.INPUT_TYPE.getName(), subType.inputType());
        fieldSubTypeJson.put(FieldTypeKeys.PLACEHOLDER.getName(), response.resolveI18nPlaceholder(subType.placeholder()));

        return fieldSubTypeJson;
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

}

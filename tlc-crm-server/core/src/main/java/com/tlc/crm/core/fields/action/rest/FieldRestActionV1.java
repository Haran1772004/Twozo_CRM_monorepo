package com.tlc.crm.core.fields.action.rest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;
import com.tlc.crm.common.fields.ApiKeys;
import com.tlc.crm.common.fields.RestApiKeys;

import com.tlc.crm.core.fields.api.model.FieldVisibilityRule;

import com.tlc.crm.entity.dropdown.AsyncDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.internal.DropdownActionContainer;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.option.OptionKeys;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.converter.FieldVisibilityGroupConverter;
import com.tlc.crm.entity.models.internal.Field;
import com.tlc.crm.entity.models.internal.FieldVisibilityCriteriaGroup;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.service.OptionService;

import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.models.settings.SourceSettings;
import com.tlc.crm.fields.service.FieldSourceService;
import com.tlc.crm.fields.service.FieldTypeService;

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
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
@RestApiHandler(path = "/v1/field")
public class FieldRestActionV1 extends AbstractSecureRestAction {

    private final FieldService fieldService;
    private final DropdownActionContainer dropdownActionContainer;
    private final FieldSourceService fieldSourceService;
    private final FieldTypeService fieldTypeService;
    private final FieldVisibilityGroupConverter fieldVisibilityGroupConverter;
    private final OptionService optionService;

    public FieldRestActionV1() {
        this.dropdownActionContainer = DropdownActionContainer.getInstance();
        this.fieldService = FieldService.getInstance();
        this.fieldSourceService = FieldSourceService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.fieldVisibilityGroupConverter = FieldVisibilityGroupConverter.getInstance();
        this.optionService = OptionService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The CRM request containing path parameters, query parameters,
     *                 headers, and contextual metadata.
     * @param response The CRM response used to write the result or error payload.
     */
    @ApiRoute(path = "/list", method = HttpMethod.GET)
    public void list(final CrmRequest request, final CrmResponse response) {
        final Long orgId = request.orgId();
        final String entitySource = Source.get(request.getParameter(FieldKeys.SOURCE.getName())).getName();
        final FieldSource fieldSource = fieldSourceService.retrieveSourceWithSettings(orgId, entitySource);

        if (fieldSource.isCommon()) {

            if (fieldSource.getSettings().isEnabled()) {
                final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, entitySource);

                if (fields.isEmpty()) {
                    throw ErrorCode.get(EntityErrorCodes.FIELDS_NOT_FOUND, "i18n.entity.fields.not.found");
                } else {
                    response.put(FieldKeys.FIELDS.getName(), convertFieldsInfoToJsonArray(orgId, request.getI18NCRMResolutionAdapter(), fields, true));
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
     * @param request  The CRM request containing the payload and request metadata.
     * @param response The CRM response used to send the updated resource or status result.
     */
    @ApiRoute(path = "/dropdown", method = HttpMethod.GET)
    public void dropdown(final CrmRequest request, final CrmResponse response) {
        final Optional<String> optionalNameParam = request.getParam(ApiKeys.NAME).findFirst();

        if (optionalNameParam.isPresent()) {
            final DropdownRequest dropdownRequest = new DropdownRequest(request);
            final String name = optionalNameParam.get();
            final Optional<FieldInfo> fieldByInternalName = fieldService.getFieldByInternalName(request.orgId(), name);

            final BiConsumer<String, FieldInfo> dropdownDataFetcher = (dropdown, field) -> {
                final AsyncDropdownAction asyncDropdownAction = dropdownActionContainer.fetchAsyncAction(dropdown);

                if (Objects.isNull(asyncDropdownAction)) {
                    final DropdownAction dropdownAction = dropdownActionContainer.fetchAction(dropdown);

                    loadOptions(request, Objects.isNull(dropdown) ? field : null, dropdownAction, response);
                } else {
                    final String pageParam = request.getParameter(ApiKeys.PAGE);
                    final String sizeParam = request.getParameter(ApiKeys.SIZE);
                    final int page = Objects.isNull(pageParam) ? NumericalConstant.ONE : Integer.parseInt(pageParam);
                    final int limit = Objects.isNull(sizeParam) ? NumericalConstant.ONE_HUNDRED : Integer.parseInt(sizeParam);
                    final int start = ((page - 1) * limit) + 1;
                    final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
                    final DropdownCriteria dropdownCriteria = new DropdownCriteria();
                    final Object criteriaValue = request.getParameter(ApiKeys.VALUE);
                    final String criteriaType = request.getParameter(ApiKeys.TYPE);

                    if (Objects.nonNull(criteriaValue) && Objects.nonNull(criteriaType)) {
                        dropdownCriteria.setCriteriaValue(criteriaValue);
                        dropdownCriteria.setCriteriaType(criteriaType);
                    }
                    dropdownRequest.setLimit(dropdownLimit);
                    dropdownRequest.setCriteria(dropdownCriteria);
                    dropdownRequest.setClientType(CrmRequest.ClientType.REST_API);
                    asyncDropdownAction.fetchAndDispatchData(dropdownRequest, response);
                }
            };

            if (fieldByInternalName.isPresent()) {
                final FieldInfo field = fieldByInternalName.get();
                final StoreType storeType = field.storeType();

                if (field.isPersistent()) {
                    final DropdownAction dropdownAction;

                    if (StoreType.DROPDOWN == storeType || StoreType.MULTI_SELECT == storeType) {
                        dropdownAction = dropdownActionContainer.fetchAction(field);
                    } else {
                        dropdownAction = dropdownActionContainer.fetchAction(switch (storeType) {
                            case CONTACT -> "contactList";
                            case COMPANY -> "companyList";
                            case OWNER -> "userList";
                            default ->
                                    throw ErrorCode.get(EntityErrorCodes.FIELD_IS_NOT_A_DROPDOWN, "i18n.entity.field.not.dropdown");
                        });
                    }
                    loadOptions(request, field, dropdownAction, response);
                } else {
                    final String dropdown = field.presetConfig().dropdown();

                    dropdownDataFetcher.accept(dropdown, field);
                }
            } else {
                dropdownDataFetcher.accept(name, null);
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
        final BiConsumer<String, String> relatedFieldConsumer = (source, aliasName) -> {
            final Map<String, FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(source, aliasName);
            final JsonObject relatedFieldJson = Json.object();

            relatedFieldJson.put(RestApiKeys.ID, fields.get(aliasName).id());
            relatedFieldJson.put(RestApiKeys.SOURCE, source);
            jsonObject.put(RestApiKeys.RELATED_FIELD, relatedFieldJson);
        };

        if (Objects.nonNull(fieldVisibilityCriteriaGroup)) {
            fieldVisibilityGroupConverter.loadCriteriaGroupIntoJson(orgId, presetConfigJson, fieldVisibilityCriteriaGroup, i18NResolutionAdapter, fieldVisibilityRule.fieldVisibilityKey());
        }

        final String patternKey = FieldKeys.PATTERN.getName();

        if (presetConfigJson.containsKey(patternKey)) {
            final JsonObject validationJson = presetConfigJson.getJsonObject(patternKey);
            final String violationErrorKey = RestApiKeys.VIOLATION_MESSAGE;

            if (validationJson.containsKey(violationErrorKey)) {
                final String violationError = validationJson.getString(violationErrorKey);

                validationJson.put(violationErrorKey, i18NResolutionAdapter.get(violationError));
            }
            presetConfigJson.put(FieldKeys.VALIDATIONS.getName(), validationJson);
        }

        if (presetConfigJson.containsKey(RestApiKeys.REFERRER_FIELDS)) {
            final Collection<String> fieldNames = new ArrayList<>();

            presetConfigJson.getJsonArray(RestApiKeys.REFERRER_FIELDS).forEach(fieldName -> fieldNames.add((String) fieldName));
            final Collection<FieldInfo> fields = fieldService.groupDefaultFieldsByAliasName(field.fieldSource().getName(), fieldNames).values();
            final JsonArray referrerFieldsArray = Json.array();
            final String fieldsKey = FieldKeys.ID.getName();
            final String fieldTypeKey = FieldKeys.FIELD_TYPE.getName();

            for (final FieldInfo fieldInfo : fields) {
                referrerFieldsArray.put(Json.object().put(fieldsKey, fieldInfo.id()).put(fieldTypeKey, fieldInfo.storeType()));
            }

            presetConfigJson.remove(RestApiKeys.REFERRER_FIELDS);
            jsonObject.put(RestApiKeys.REFERRER_FIELDS, referrerFieldsArray);
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
        presetConfig.getMaxValue().ifPresent(value -> presetConfigJson.put(FieldKeys.MAX_DATE.getName(), value));

        jsonObject.put(RestApiKeys.SOURCE_ID, sourceId);
        jsonObject.put(RestApiKeys.FIELD_NAME, i18NResolutionAdapter.get(field.i18nName()));
        jsonObject.put(RestApiKeys.INTERNAL_NAME, field.getInternalName());
        jsonObject.put(RestApiKeys.IS_ACTIVE, field.isActive());
        jsonObject.put(RestApiKeys.CONFIG, getPresetConfig(presetConfigJson));
        jsonObject.put(RestApiKeys.ID, field.id());

        loadFieldOptions(orgId, field, jsonObject, i18NResolutionAdapter);
        final JsonArray subfields = buildSubFieldsJson(field, false, i18NResolutionAdapter, false);

        if (Objects.nonNull(subfields)) {
            jsonObject.put(RestApiKeys.SUB_FIELDS, subfields);
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
     * Gets the preset configuration from the provided JSON object and returns a new JSON object containing the relevant validation configurations.
     *
     * @param presetConfigJson Refers to the JSON object containing the preset configuration.
     * @return The JSON object containing the preset configuration.
     */
    private JsonObject getPresetConfig(final JsonObject presetConfigJson) {
        final JsonObject validationConfig = Json.object();
        final String maxKey = PresetConfig.Configuration.MAX.getName();
        final String minKey = PresetConfig.Configuration.MIN.getName();
        final String minLengthKey = PresetConfig.Configuration.MIN_LENGTH.getName();
        final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
        final String maxLimitKey = PresetConfig.Configuration.MAX_LIMIT.getName();
        final String maxDateKey = FieldKeys.MAX_DATE.getName();

        if (presetConfigJson.containsKey(minKey)) {
            validationConfig.put(minKey, presetConfigJson.getInt(minKey));
        }

        if (presetConfigJson.containsKey(maxKey)) {
            validationConfig.put(maxKey, presetConfigJson.getLong(maxKey));
        }

        if (presetConfigJson.containsKey(minLengthKey)) {
            validationConfig.put(RestApiKeys.MIN_LENGTH, presetConfigJson.getInt(minLengthKey));
        }

        if (presetConfigJson.containsKey(maxLengthKey)) {
            validationConfig.put(RestApiKeys.MAX_LENGTH, presetConfigJson.getInt(maxLengthKey));
        }

        if (presetConfigJson.containsKey(maxLimitKey)) {
            validationConfig.put(RestApiKeys.MAX_LIMIT, presetConfigJson.getInt(maxLimitKey));
        }

        if (presetConfigJson.containsKey(maxDateKey)) {
            validationConfig.put(RestApiKeys.MAX_DATE, presetConfigJson.getInt(maxDateKey));
        }

        return validationConfig;
    }

    /**
     * Loads the field options for the given field and adds them to the provided JsonObject.
     *
     * @param orgId                 Refers the organization id.
     * @param fieldInfo             Refers the field information.
     * @param jsonObject            Refers the JsonObject to which the options will be added.
     * @param i18nResolutionAdapter Refers the {@code I18nResolutionAdapter} for resolving internationalized option names.
     */
    private void loadFieldOptions(final long orgId, final FieldInfo fieldInfo, final JsonObject jsonObject, final I18nResolutionAdapter i18nResolutionAdapter) {
        if (StoreType.isSelectorType(fieldInfo.storeType())) {
            final DropdownType dropdownType = fieldInfo.presetConfig().dropdownType();

            if (dropdownType == DropdownType.DYNAMIC || dropdownType == DropdownType.STATIC || dropdownType == DropdownType.HYBRID) {
                final JsonArray array = Json.array();
                final Collection<Option> options = optionService.get(orgId, fieldInfo);

                for (final Option option : options) {
                    final JsonObject optionJson = Json.object();

                    optionJson.put(OptionKeys.ID, option.id());
                    optionJson.put(OptionKeys.NAME, i18nResolutionAdapter.get(option.getOption()));
                    array.put(optionJson);
                }
                jsonObject.put(RestApiKeys.OPTIONS, array);
            }
        }
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

                subTypeJson.put(RestApiKeys.ID, fieldSubType.id());
                subTypeJson.put(RestApiKeys.NAME, fieldSubType.name());
                subTypeJson.put(RestApiKeys.TYPE, fieldSubType.fieldType().storeType().getName());
                subTypeJson.put(RestApiKeys.DISPLAY_NAME, i18NResolutionAdapter.get((fieldSubType.displayName())));

                if (Objects.nonNull(config)) {
                    subTypeJson.put(RestApiKeys.CONFIG, Json.object(config));
                }
                subTypeJson.put(RestApiKeys.INPUT_TYPE, fieldSubType.inputType());
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

        source.put(RestApiKeys.NAME, fieldSource.getName());
        source.put(RestApiKeys.ID, fieldSource.id());
        source.put(RestApiKeys.IS_ENABLED, isEnabled);

        if (isEnabled && null != sourceConfig) {
            source.put(RestApiKeys.CONFIG, Json.object(sourceConfig));
        }

        return source;
    }

    /**
     * Loads the dropdown Options.
     *
     * @param request        Holds the request needed to dropdown.
     * @param field          Represents the field related information.
     * @param dropdownAction Dropdown action
     * @param crmResponse    Refers the response to be sent.
     */
    private void loadOptions(final CrmRequest request, final FieldInfo field,
                             final DropdownAction dropdownAction, final CrmResponse crmResponse) {
        final DropdownRequest dropdownRequest = new DropdownRequest(request);
        final OptionContainer optionContainer = fetchOptionContainer(request, dropdownRequest, field, dropdownAction);
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final long totalRecords = optionContainer.getTotalRecords();

        crmResponse.put(RestApiKeys.TOTAL_RECORDS, totalRecords);
        crmResponse.put(RestApiKeys.HAS_MORE, dropdownLimit.start() + dropdownLimit.limit() <= totalRecords);
        crmResponse.put(RestApiKeys.OPTIONS, optionContainer.getOptions());
        crmResponse.put(RestApiKeys.SIZE, optionContainer.size());
    }

    /**
     * Fetches the {@link OptionContainer} for the requested dropdown values.
     *
     * @param dropdownRequest Holds the request needed to dropdown.
     * @param field           represents the field related information.
     * @param dropdownAction  dropdownAction.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    private OptionContainer fetchOptionContainer(final CrmRequest request, final DropdownRequest dropdownRequest, final FieldInfo field, final DropdownAction dropdownAction) {
        final String pageParam = request.getParameter(ApiKeys.PAGE);
        final String sizeParam = request.getParameter(ApiKeys.SIZE);
        final int page = Objects.isNull(pageParam) ? NumericalConstant.ONE : Integer.parseInt(pageParam);
        final int limit = Objects.isNull(sizeParam) ? NumericalConstant.ONE_HUNDRED : Integer.parseInt(sizeParam);
        final int start = ((page - 1) * limit) + 1;
        final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();
        final Object criteriaValue = request.getParameter(ApiKeys.VALUE);
        final String criteriaType = request.getParameter(ApiKeys.TYPE);

        if (Objects.nonNull(criteriaValue) && Objects.nonNull(criteriaType)) {
            dropdownCriteria.setCriteriaValue(criteriaValue);
            dropdownCriteria.setCriteriaType(criteriaType);
        }
        dropdownRequest.setLimit(dropdownLimit);
        dropdownRequest.setCriteria(dropdownCriteria);
        dropdownRequest.setField(field);
        dropdownRequest.setClientType(CrmRequest.ClientType.REST_API);

        if (CrmRequest.ClientType.MOBILE == dropdownRequest.getClientType()) {
            return dropdownAction.fetchMobileData(dropdownRequest);
        } else {
            return dropdownAction.fetchData(dropdownRequest);
        }
    }
}

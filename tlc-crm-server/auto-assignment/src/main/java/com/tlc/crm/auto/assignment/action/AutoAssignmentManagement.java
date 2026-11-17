package com.tlc.crm.auto.assignment.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.commons.page.Page;
import com.tlc.commons.functions.TriConsumer;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.models.type.DropdownType;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;

import com.tlc.crm.auto.assignment.api.model.AssignmentRuleUser;
import com.tlc.crm.auto.assignment.api.converter.AutoAssignmentConverter;
import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentManager;
import com.tlc.crm.auto.assignment.api.model.AssignmentModule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentRule;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentCriteriaField;
import com.tlc.crm.auto.assignment.api.model.CriteriaDropdownInfo;
import com.tlc.crm.auto.assignment.cache.service.AutoAssignmentComparatorService;
import com.tlc.crm.auto.assignment.fields.AutoAssignmentAttributes;
import com.tlc.crm.auto.assignment.handler.AutoAssignmentPageHandler;
import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.i18n.I18nResolver;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the web actions of {@code AutoAssignment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/assignment/rule/mgmt")
public class AutoAssignmentManagement extends CrmConfigAction<AutoAssignmentRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentManagement.class);

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 250;

    private final AutoAssignmentManager autoAssignmentManager;
    private final AutoAssignmentComparatorService assignmentComparatorService;
    private final FieldService fieldService;
    private final FieldTypeService fieldTypeService;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final AutoAssignmentConverter autoAssignmentConverter;
    private final TerritoryManager territoryManager;
    private final TeamManager teamManager;

    public AutoAssignmentManagement() {
        this.autoAssignmentManager = AutoAssignmentManager.getInstance();
        this.assignmentComparatorService = AutoAssignmentComparatorService.getInstance();
        this.fieldService = FieldService.getSecureInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.autoAssignmentConverter = AutoAssignmentConverter.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
        this.teamManager = TeamManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the incoming {@link CrmRequest}.
     * @param response Refers the {@link CrmResponse} to be sent.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(AutoAssignmentAttributes.DATA);
        final String type = requestJson.getString(AutoAssignmentAttributes.TYPE);
        final long orgId = request.orgId();

        switch (type) {
            case "updateStatus" -> updateStatus(orgId, data, response);
            case "criteriaFieldList" -> processCriteriaFields(orgId, data, request, response);
            case "list" ->
                    response.put(AutoAssignmentAttributes.RULES, getAssignmentRules(orgId, data, request, response));
            case "updateOrder" -> updateOrder(orgId, data);
            case "territoryList" ->
                    response.put(AutoAssignmentAttributes.OPTIONS, getTerritoryList(orgId, data, response));
            case "getTeamsUsersTerritories" -> fetchAndDispatchTeamsUsersTerritories(orgId, data, response);
            case "getUsers" -> dispatchUsers(orgId, data, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<AutoAssignmentRule> getConfigManager() {
        return autoAssignmentManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Crm request
     * @param jsonObject Holds entity details
     * @return Converted entity
     */
    @Override
    public AutoAssignmentRule convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return autoAssignmentConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AutoAssignmentRule convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return autoAssignmentConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request            The received request.
     * @param autoAssignmentRule Refers the {@link AutoAssignmentRule}.
     * @return The {@link JsonObject} of auto-assignment data.
     */
    @Override
    public JsonObject convert(final CrmRequest request, final AutoAssignmentRule autoAssignmentRule) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return autoAssignmentConverter.convert(converterConfig, autoAssignmentRule);
    }

    /**
     * {@inheritDoc}
     *
     * @param request            Holds the {@link CrmRequest} details
     * @param autoAssignmentRule Represents the model details
     * @param crmResponse        Represents the {@link CrmResponse}
     */
    @Override
    public void fetchAndDispatchUser(final CrmRequest request, final AutoAssignmentRule autoAssignmentRule, final CrmResponse crmResponse) {
        final Long orgId = autoAssignmentRule.orgId();
        final Collection<Long> userIds = autoAssignmentRule.getAssignmentRuleUsers().stream().map(assignmentRuleUser -> assignmentRuleUser.getCrmUser().id()).toList();

        if (userIds.isEmpty()) {
            convert(request, autoAssignmentRule).forEach(crmResponse::put);
        } else {
            crmResponse.convertIntoAsync();

            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> crmUsers = users.stream().collect(Collectors.toMap(User::id, Function.identity()));

                        for (final AssignmentRuleUser assignmentRuleUser : autoAssignmentRule.getAssignmentRuleUsers()) {
                            final CrmUser crmUser = assignmentRuleUser.getCrmUser();

                            crmUser.setUser(crmUsers.get(crmUser.getIamId()));
                        }
                        convert(request, autoAssignmentRule).forEach(crmResponse::put);
                    } catch (Exception exception) {
                        crmResponse.setErrorCode(ErrorCode.get(AutoAssignmentErrorCodes.FAILED_TO_RETRIEVE_DATA, "i18n.auto.assignment.retrieval.failed", exception));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    crmResponse.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                    crmResponse.send();
                }
            };
            userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
        }
    }

    /**
     * Get assignment rules for the given organization id and request data.
     *
     * @param orgId    The organization id.
     * @param data     The input data as JsonObject.
     * @param request  The CrmRequest object containing request details.
     * @param response The CrmResponse object to populate response.
     * @return A JsonArray containing the assignment rules.
     */
    private JsonArray getAssignmentRules(final long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = Math.max(data.optInt(AutoAssignmentAttributes.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(AutoAssignmentAttributes.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final AutoAssignmentPageHandler autoAssignmentPageHandler = new AutoAssignmentPageHandler(start, limit);

        autoAssignmentPageHandler.setOrgId(orgId);
        final long totalRecords = autoAssignmentPageHandler.totalRecordsCount();

        if (totalRecords > 0 && start <= totalRecords) {
            final Collection<AutoAssignmentRule> autoAssignmentRules = autoAssignmentPageHandler.fetchPageData().getData();
            final I18nResolver i18nResolver = request.i18nResolver();

            for (final AutoAssignmentRule autoAssignmentRule : autoAssignmentRules) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(AutoAssignmentAttributes.ID, autoAssignmentRule.id());
                jsonObject.put(AutoAssignmentAttributes.NAME, i18nResolver.get(autoAssignmentRule.getName()));
                jsonObject.put(AutoAssignmentAttributes.DESCRIPTION, i18nResolver.get(autoAssignmentRule.getDescription()));
                jsonObject.put(AutoAssignmentAttributes.ORDER, autoAssignmentRule.getOrder());
                jsonObject.put(AutoAssignmentAttributes.MODULE, autoAssignmentRule.getAssignmentModule().getId());
                jsonObject.put(AutoAssignmentAttributes.UPDATED_BY, autoAssignmentRule.getUpdatedBy().getFullName());
                jsonObject.put(AutoAssignmentAttributes.IS_ACTIVE, autoAssignmentRule.isActive());
                jsonObject.put(AutoAssignmentAttributes.UPDATED_AT, autoAssignmentRule.getModifiedTime().getTimeStamp());

                jsonArray.put(jsonObject);
            }
        }

        response.put(AutoAssignmentAttributes.TOTAL_RECORDS, totalRecords);
        response.put(AutoAssignmentAttributes.HAS_MORE, autoAssignmentPageHandler.hasMore());

        return jsonArray;
    }

    /**
     * Update the order for the given organization id with provided data.
     *
     * @param orgId      The organization id.
     * @param jsonObject The JsonObject containing order details.
     */
    private void updateOrder(final long orgId, final JsonObject jsonObject) {
        final Set<Long> assignmentIds = new LinkedHashSet<>();

        jsonObject.getJsonArray(AutoAssignmentAttributes.IDS).forEach(assignmentId -> assignmentIds.add((Long) assignmentId));

        if (!autoAssignmentManager.exists(orgId, assignmentIds)) {
            throw ErrorCode.get(AutoAssignmentErrorCodes.AUTO_ASSIGNMENT_NOT_FOUND, "i18n.auto.assignment.not.found");
        }
        autoAssignmentManager.updateOrder(orgId, assignmentIds);
    }

    /**
     * <p>
     * Process the criteria fields into the {@link CrmResponse} based on the provided data.
     * </p>
     *
     * @param orgId       Represents the organization ID.
     * @param data        Refers the request json data.
     * @param crmRequest  Holds the {@link CrmRequest} details.
     * @param crmResponse Refers the response to be sent.
     */
    private void processCriteriaFields(final long orgId, final JsonObject data, CrmRequest crmRequest, CrmResponse crmResponse) {
        final AssignmentModule assignmentModule = AssignmentModule.get(data.getShort(AutoAssignmentAttributes.MODULE));
        final int start = Math.max(data.optInt(NoteFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE) - 1;
        final int limit = Math.min(data.optInt(NoteFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final I18nResolver i18nResolver = crmRequest.i18nResolver();
        final I18NCRMResolutionAdapter i18NCRMResolutionAdapter = crmRequest.getI18NCRMResolutionAdapter();
        final Collection<AutoAssignmentCriteriaField> fieldList = getFields(orgId, assignmentModule);
        final Collection<AutoAssignmentCriteriaField> fields;
        final int totalRecords = fieldList.size();
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        if (data.containsKey(AutoAssignmentAttributes.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(AutoAssignmentAttributes.CRITERIA);

            dropdownCriteria.setCriteriaType(criteriaJson.getString(AutoAssignmentAttributes.TYPE));
            dropdownCriteria.setCriteriaValue(criteriaJson.getString(AutoAssignmentAttributes.VALUE));
        }

        if (dropdownCriteria.getValue() instanceof final String criteriaFieldValue) {
            final String name = criteriaFieldValue.toUpperCase();

            fields = fieldList.stream().filter(autoAssignmentCriteriaField -> {
                final String fieldName = i18NCRMResolutionAdapter.get(autoAssignmentCriteriaField.getName()).toUpperCase();

                return switch (dropdownCriteria.getCriteriaType()) {
                    case STARTS_WITH -> fieldName.startsWith(name);
                    case CONTAINS -> fieldName.contains(name);
                    default -> fieldName.equalsIgnoreCase(criteriaFieldValue);
                };
            }).skip(start).limit(limit).toList();
        } else {
            fields = fieldList.stream().skip(start).limit(limit).toList();
        }

        if (!fields.isEmpty()) {
            final JsonArray criteriaFieldsArray = Json.array();

            for (final AutoAssignmentCriteriaField autoAssignmentCriteriaField : fields) {
                criteriaFieldsArray.put(convertFieldToJson(autoAssignmentCriteriaField, i18NCRMResolutionAdapter));
            }
            crmResponse.put(AutoAssignmentAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
        }

        final Map<Long, AutoAssignmentComparator> comparators = assignmentComparatorService.getAll();

        if (!comparators.isEmpty()) {
            final JsonObject comparatorJson = Json.object();

            for (final AutoAssignmentComparator assignmentComparator : comparators.values()) {
                comparatorJson.put(assignmentComparator.getId().toString(), convertComparatorToJson(assignmentComparator, i18nResolver));
            }
            crmResponse.put(AutoAssignmentAttributes.COMPARATOR, comparatorJson);
        }
        crmResponse.put(AutoAssignmentAttributes.TOTAL_RECORDS, totalRecords);
        crmResponse.put(AutoAssignmentAttributes.HAS_MORE, (start + limit) <= totalRecords);
    }

    /**
     * Get the collection of auto assignment criteria fields for the specified organization and module.
     *
     * @param orgId            The organization id.
     * @param assignmentModule The assignment module to retrieve fields for.
     * @return A collection of AutoAssignmentCriteriaField objects.
     */
    private Collection<AutoAssignmentCriteriaField> getFields(final long orgId, final AssignmentModule assignmentModule) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, assignmentModule.source().getName())
                .stream().filter(field -> !isRestrictedStoreType(field.storeType()) && field.isActive())
                .collect(Collectors.toUnmodifiableSet());
        final Collection<AutoAssignmentCriteriaField> autoAssignmentCriteriaFields = new ArrayList<>();
        final Map<StoreType, FieldType> fieldTypeMap = fieldTypeService.getAllFieldTypes().stream().collect(Collectors.toMap(FieldType::storeType, Function.identity()));

        for (final FieldInfo fieldInfo : fields) {

            if (!Objects.equals(AutoAssignmentAttributes.ID, fieldInfo.aliasName())) {
                final AutoAssignmentCriteriaField autoAssignmentCriteriaField = new AutoAssignmentCriteriaField();

                autoAssignmentCriteriaField.setCriteriaDropdownInfo(getDropdownInfo(fieldInfo));
                autoAssignmentCriteriaField.setName(fieldInfo.i18nName());
                autoAssignmentCriteriaField.setFieldId(fieldInfo.id());
                autoAssignmentCriteriaField.setAliasName(fieldInfo.aliasName());
                autoAssignmentCriteriaField.setSourceId(fieldInfo.sourceId());
                autoAssignmentCriteriaField.setFieldType(fieldTypeMap.get(Optional.ofNullable(fieldInfo.presetConfig().getAutomationConfig())
                        .map(config -> StoreType.valueOf(config.getString(AutoAssignmentAttributes.FIELD_TYPE))).orElseGet(fieldInfo::storeType)));
                autoAssignmentCriteriaField.setFieldConfig(fieldInfo.presetConfig().presetConfigAsJson());

                autoAssignmentCriteriaFields.add(autoAssignmentCriteriaField);
            }
        }

        return autoAssignmentCriteriaFields.stream().sorted(Comparator.comparing(AutoAssignmentCriteriaField::getFieldId)).toList();
    }

    /**
     * <p>
     * Gets the {@link CriteriaDropdownInfo} for the given {@link FieldInfo}
     * </p>
     *
     * @param fieldInfo Represents the {@link FieldInfo}
     * @return the {@link CriteriaDropdownInfo} for the given {@link FieldInfo}
     */
    private CriteriaDropdownInfo getDropdownInfo(final FieldInfo fieldInfo) {
        final PresetConfig presetConfig = fieldInfo.presetConfig();
        final String aliasName = fieldInfo.aliasName();
        final String dropdown = switch (Objects.isNull(aliasName) ? fieldInfo.fieldName() : aliasName) {
            case AutoAssignmentAttributes.PIPELINE_STAGE -> AutoAssignmentAttributes.STAGE_LIST;
            case AutoAssignmentAttributes.LAST_ACTIVITY_TYPE -> AutoAssignmentAttributes.ACTIVITY_TYPE_LIST;
            default -> presetConfig.dropdown();
        };
        final StoreType storeType = fieldInfo.storeType();

        if ((StoreType.isSelectorType(storeType) || storeType.isEntityType() || storeType.isOwnerType()) || Objects.nonNull(dropdown)) {
            final CriteriaDropdownInfo criteriaDropdownInfo = new CriteriaDropdownInfo();

            if (Objects.equals(presetConfig.dropdownType(), DropdownType.STATIC)) {
                criteriaDropdownInfo.setStatic(true);
            }

            if (Objects.nonNull(presetConfig.dependsOn())) {
                criteriaDropdownInfo.addRequiredKey(AutoAssignmentAttributes.DEPENDENT_ID);
            }

            switch (storeType) {
                case OWNER -> criteriaDropdownInfo.setDropdownName(AutoAssignmentAttributes.USER_LIST);
                case COMPANY, COMPANY_ONBOARD ->
                        criteriaDropdownInfo.setDropdownName(AutoAssignmentAttributes.COMPANY_LIST);
                case CONTACT, CONTACT_ONBOARD, CONTACTS_ONBOARD ->
                        criteriaDropdownInfo.setDropdownName(AutoAssignmentAttributes.CONTACT_LIST);
                default -> {
                    criteriaDropdownInfo.setDropdownName(dropdown);
                    criteriaDropdownInfo.addRequiredKey(AutoAssignmentAttributes.ID);
                }
            }

            return criteriaDropdownInfo;
        }
        return null;
    }

    /**
     * <p>
     * Converts the {@link AutoAssignmentCriteriaField} to {@link JsonObject}
     * </p>
     *
     * @param autoAssignmentCriteriaField Represents the {@link AutoAssignmentCriteriaField}
     * @param i18NCRMResolutionAdapter    Represents the {@link I18NCRMResolutionAdapter}
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldToJson(final AutoAssignmentCriteriaField autoAssignmentCriteriaField, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter) {
        final JsonObject jsonObject = Json.object();
        final FieldType fieldType = autoAssignmentCriteriaField.getFieldType();
        final CriteriaDropdownInfo criteriaDropdownInfo = autoAssignmentCriteriaField.getCriteriaDropdownInfo();
        final Collection<Long> comparatorIds = assignmentComparatorService.getComparatorsByFieldTypeId(fieldType.id())
                .stream().map(AutoAssignmentComparator::getId).sorted(Comparator.naturalOrder()).toList();

        if (Objects.nonNull(criteriaDropdownInfo)) {
            final JsonObject criteriaDropdownJson = Json.object();

            criteriaDropdownJson.put(AutoAssignmentAttributes.NAME, criteriaDropdownInfo.getDropdownName())
                    .put(AutoAssignmentAttributes.REQUIRED_KEYS, criteriaDropdownInfo.getRequiredKeys());
            jsonObject.put(AutoAssignmentAttributes.DROPDOWN, criteriaDropdownJson);
        }

        jsonObject.put(AutoAssignmentAttributes.FIELD_ID, autoAssignmentCriteriaField.getFieldId())
                .put(AutoAssignmentAttributes.NAME, i18NCRMResolutionAdapter.get(autoAssignmentCriteriaField.getName()))
                .put(AutoAssignmentAttributes.FIELD_TYPE, getFieldTypeName(autoAssignmentCriteriaField)).put(AutoAssignmentAttributes.COMPARATORS, comparatorIds);
        final JsonObject validationConfig = getValidationConfig(autoAssignmentCriteriaField, i18NCRMResolutionAdapter);

        if (0 < validationConfig.size()) {
            jsonObject.put(AutoAssignmentAttributes.CONFIG, validationConfig);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the validation config of the {@link AutoAssignmentCriteriaField}.
     * </p>
     *
     * @param autoAssignmentCriteriaField Represents the auto-assignment criteria field
     * @param i18NCRMResolutionAdapter    Refers the {@code I18NCRMResolutionAdapter} to resolve the i18n keys.
     * @return the json which contains the validation config
     */
    private JsonObject getValidationConfig(final AutoAssignmentCriteriaField autoAssignmentCriteriaField, final I18NCRMResolutionAdapter i18NCRMResolutionAdapter) {
        final JsonObject fieldConfig = autoAssignmentCriteriaField.getFieldConfig();
        final JsonObject validationConfig = Json.object();
        final String maxKey = PresetConfig.Configuration.MAX.getName();
        final String minKey = PresetConfig.Configuration.MIN.getName();
        final String minLengthKey = PresetConfig.Configuration.MIN_LENGTH.getName();
        final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
        final String maxLimitKey = PresetConfig.Configuration.MAX_LIMIT.getName();
        final String validationKey = PresetConfig.Configuration.PATTERN.getName();
        final String isDefaultRuleCriteriaField = PresetConfig.Configuration.IS_DEFAULT_RULE_CRITERIA_FIELD.getName();

        if (fieldConfig.containsKey(minKey)) {
            validationConfig.put(minKey, fieldConfig.getInt(minKey));
        }

        if (fieldConfig.containsKey(maxKey)) {
            validationConfig.put(maxKey, fieldConfig.getLong(maxKey));
        }

        if (fieldConfig.containsKey(minLengthKey)) {
            validationConfig.put(minLengthKey, fieldConfig.getInt(minLengthKey));
        }

        if (fieldConfig.containsKey(maxLengthKey)) {
            validationConfig.put(maxLengthKey, fieldConfig.getInt(maxLengthKey));
        }

        if (fieldConfig.containsKey(maxLimitKey)) {
            validationConfig.put(maxLimitKey, fieldConfig.getInt(maxLimitKey));
        }

        if (fieldConfig.containsKey(isDefaultRuleCriteriaField)) {
            validationConfig.put(AutoAssignmentAttributes.IS_DEFAULT, fieldConfig.optBoolean(isDefaultRuleCriteriaField));
        }

        if (fieldConfig.containsKey(validationKey)) {
            final JsonObject validationJson = fieldConfig.getJsonObject(validationKey);
            final String violationMessageKey = FieldKeys.VIOLATION_MESSAGE.getName();
            final String valueKey = FieldKeys.VALUE.getName();
            final String messageKey = FieldKeys.MESSAGE.getName();
            final String violationMessage;

            if (validationJson.containsKey(violationMessageKey)) {
                violationMessage = validationJson.getString(violationMessageKey);
            } else if (validationJson.containsKey(messageKey)) {
                violationMessage = validationJson.getString(messageKey);
            } else {
                violationMessage = null;
            }

            if (Objects.nonNull(violationMessage)) {
                validationJson.put(violationMessageKey, i18NCRMResolutionAdapter.get(violationMessage));
                validationJson.remove(messageKey);
            }

            if (validationJson.containsKey(valueKey)) {
                validationJson.put(FieldKeys.REGEX.getName(), validationJson.get(valueKey));
                validationJson.remove(valueKey);
            }
            validationConfig.put(PresetConfig.Configuration.VALIDATION.getName(), validationJson);
        }

        return validationConfig;
    }

    /**
     * Gets the field type name of the {@link AutoAssignmentCriteriaField}.
     *
     * @param autoAssignmentCriteriaField Refers the {@link AutoAssignmentCriteriaField}.
     * @return The field type name.
     */
    private String getFieldTypeName(final AutoAssignmentCriteriaField autoAssignmentCriteriaField) {
        final Optional<String> optionalAliasName = Optional.ofNullable(autoAssignmentCriteriaField.getAliasName());
        final String fieldType = autoAssignmentCriteriaField.getFieldType().name();

        return optionalAliasName.map(aliasName -> switch (aliasName) {
            case AutoAssignmentAttributes.PIPELINE_STAGE -> AutoAssignmentAttributes.MULTI_DROPDOWN;
            case AutoAssignmentAttributes.TITLE -> StoreType.SINGLE_STRING.getName();
            case AutoAssignmentAttributes.PRODUCTS, AutoAssignmentAttributes.DEALS,
                 AutoAssignmentAttributes.WON_DEALS, AutoAssignmentAttributes.CLOSED_DEALS,
                 AutoAssignmentAttributes.LOST_DEALS -> StoreType.SINGLE_NUMBER.getName();
            default -> fieldType;
        }).orElseGet(() -> fieldType);
    }

    /**
     * <p>
     * Convert {@link AutoAssignmentComparator} details to {@link JsonObject}.
     * </p>
     *
     * @param autoAssignmentComparator It holds the {@link AutoAssignmentComparator} details.
     * @param i18nResolver             Refers the {@link I18nResolver} for resolve the i18n data.
     * @return The converted comparator {@link JsonObject}.
     */
    private JsonObject convertComparatorToJson(final AutoAssignmentComparator autoAssignmentComparator, final I18nResolver i18nResolver) {
        final JsonObject comparatorJson = Json.object();

        comparatorJson.put(AutoAssignmentAttributes.NAME, i18nResolver.get(autoAssignmentComparator.getName()));
        comparatorJson.put(AutoAssignmentAttributes.HAS_VALUE, autoAssignmentComparator.hasValue());

        if (Objects.equals(AutoAssignmentAttributes.ANY, autoAssignmentComparator.getAliasName())) {
            comparatorJson.put(AutoAssignmentAttributes.TYPE, AutoAssignmentAttributes.MULTI_SELECT);
        }

        return comparatorJson;
    }

    /**
     * <p>
     * Checks whether the given {@link StoreType} is restricted for auto-assignment fields or not.
     * </p>
     *
     * @param storeType Represents the {@link StoreType}.
     * @return true if the store type matched with auto-assignment criteria fields store type.
     */
    private boolean isRestrictedStoreType(final StoreType storeType) {
        return switch (storeType) {
            case TIME_RANGE, DATE_TIME_RANGE, DATE_RANGE -> true;
            default -> false;
        };
    }

    /**
     * Updates the status of the {@link AutoAssignmentRule}.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param response Refers the response to be sent.
     */
    private void updateStatus(final long orgId, final JsonObject data, final CrmResponse response) {
        final long id = data.getLong(AutoAssignmentAttributes.ID);

        if (autoAssignmentManager.exists(orgId, id)) {
            autoAssignmentManager.updateStatus(orgId, id, data.getBoolean(AutoAssignmentAttributes.IS_ACTIVE));

            response.put(AutoAssignmentAttributes.MESSAGE, response.resolveI18nString("i18n.auto.assignment.status.update.success"));
        } else {
            throw ErrorCode.get(AutoAssignmentErrorCodes.AUTO_ASSIGNMENT_NOT_FOUND, "i18n.auto.assignment.not.found");
        }
    }

    /**
     * Retrieves the list of territories for a given organization.
     *
     * @param orgId    The ID of the organization for which territories are requested.
     * @param data     Additional input data required for fetching territories.
     * @param response The CrmResponse object used to capture any response or status.
     * @return A JsonArray containing the territories.
     */
    private JsonArray getTerritoryList(final long orgId, final JsonObject data, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final int start = data.optInt(AutoAssignmentAttributes.START, NumericalConstant.ONE);
        final int limit = data.optInt(AutoAssignmentAttributes.LIMIT, NumericalConstant.FIFTY);
        final DropdownLimit dropdownLimit = new DropdownLimit(start, limit);
        final JsonObject criteriaJson = data.optJsonObject(FieldKeys.CRITERIA.getName());
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        if (Objects.nonNull(criteriaJson)) {
            final Object criteriaValue = criteriaJson.get(FieldKeys.VALUE.getName());
            final String criteriaType = criteriaJson.getString(FieldKeys.TYPE.getName());

            dropdownCriteria.setCriteriaValue(criteriaValue);
            dropdownCriteria.setCriteriaType(criteriaType);
        }
        final Criteria criteria = dropdownCriteria.build(Table.get(OMCRMTERRITORY.TABLE).getColumn(OMCRMTERRITORY.NAME));
        final long totalRecords = territoryManager.getTotalCount(orgId, criteria);

        if (totalRecords > 0 && dropdownLimit.start() <= totalRecords) {
            final Collection<Territory> territories = territoryManager.getTerritoryDropDownList(orgId, dropdownLimit.getLimitClause(), criteria);

            for (final Territory territory : territories) {
                final JsonObject jsonObject = Json.object();

                jsonObject.put(AutoAssignmentAttributes.VALUE, territory.id());
                jsonObject.put(AutoAssignmentAttributes.NAME, territory.getName());

                jsonArray.put(jsonObject);
            }
        } else {
            response.put(AutoAssignmentAttributes.HAS_TERRITORY, territoryManager.getTotalCount(orgId) > 0);
        }
        response.put(AutoAssignmentAttributes.TOTAL_RECORDS, totalRecords);
        response.put(AutoAssignmentAttributes.HAS_MORE, (start + limit) <= totalRecords);

        return jsonArray;
    }

    /**
     * Fetches and dispatches the details of teams, users, and territories associated with the given organization.
     *
     * @param orgId       The unique identifier of the organization for which team, user, and territory details are to be fetched.
     * @param jsonObject  The JSON container to which the fetched data will be added or updated.
     * @param crmResponse The response object used to capture success, error, or validation messages during the operation.
     */
    public void fetchAndDispatchTeamsUsersTerritories(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final int start = jsonObject.optInt(AutoAssignmentAttributes.START, DEFAULT_START_VALUE);
        final int limit = jsonObject.optInt(AutoAssignmentAttributes.LIMIT, DEFAULT_LIMIT);
        final SearchFilter searchFilter = new SearchFilter();
        final SearchFilter stateFilter = new SearchFilter();
        final Collection<SearchFilter> searchFilters = new ArrayList<>();

        if (jsonObject.containsKey(AutoAssignmentAttributes.CRITERIA)) {
            final JsonObject criteriaJson = jsonObject.getJsonObject(AutoAssignmentAttributes.CRITERIA);

            searchFilter.setType(criteriaJson.getString(AutoAssignmentAttributes.TYPE));
            searchFilter.setValue(criteriaJson.get(AutoAssignmentAttributes.VALUE));
        }
        stateFilter.setType(FieldKeys.STATE.getName());
        stateFilter.setValue(UserStateFilter.ALL_USERS);
        searchFilters.add(stateFilter);
        searchFilter.setLimit(limit);
        searchFilter.setStart(start);
        searchFilter.excludeInvitedUsers(true);
        searchFilter.includeDeactivatedUsers(true);
        searchFilters.add(searchFilter);
        crmResponse.convertIntoAsync();

        final EventCallback<Page<User>> usersFetchCallBack = new EventCallback<>() {

            /**
             * {@inheritDoc}
             */
            @Override
            public void onSuccess(final Page<User> userPage) {
                try {
                    final JsonArray jsonArray = Json.array();
                    final int usersSize = userPage.getNumber();
                    final Collection<User> users = userPage.getData();

                    if (usersSize > 0 && start <= usersSize) {
                        final Collection<Long> territoryUserIds;

                        if (jsonObject.containsKey(AutoAssignmentAttributes.TERRITORY_ID)) {
                            territoryUserIds = territoryManager.getTerritoryUserIds(orgId, List.of(jsonObject.getLong(AutoAssignmentAttributes.TERRITORY_ID)));
                        } else {
                            territoryUserIds = Collections.emptySet();
                        }
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final Collection<CrmUser> crmUsers = crmUserService.getUsersByIamId(orgId, userMap.keySet());
                        final User.State activeState = User.State.ACTIVE;

                        for (final CrmUser crmUser : crmUsers) {
                            final User user = userMap.get(crmUser.getIamId());
                            final JsonObject jsonObject = Json.object();
                            final JsonObject configJson = Json.object();
                            final Long crmUserId = crmUser.id();

                            jsonObject.put(AutoAssignmentAttributes.ID, crmUserId);
                            jsonObject.put(AutoAssignmentAttributes.NAME, user.getFullName());
                            configJson.put(AutoAssignmentAttributes.IS_ACTIVE, Objects.equals(activeState, user.getUserState()));
                            jsonObject.put(AutoAssignmentAttributes.IS_TERRITORY_USER, territoryUserIds.contains(crmUserId));
                            jsonObject.put(AutoAssignmentAttributes.CONFIG, configJson);
                            jsonArray.put(jsonObject);
                        }
                        crmResponse.put(AutoAssignmentAttributes.USERS, jsonArray);
                    }
                    final long teamSize = teamManager.getTeamsCount(orgId, searchFilter);
                    final int requiredSize = start + limit - 1;

                    if (usersSize < requiredSize) {
                        final int teamStart = usersSize == 0 ? start : start > usersSize ? start - usersSize : DEFAULT_START_VALUE;
                        final int teamLimit = requiredSize - usersSize;

                        if (teamStart <= teamSize) {
                            searchFilter.setStart(teamStart);
                            searchFilter.setLimit(teamLimit);
                            final JsonArray teams = Json.array();

                            for (final Team team : teamManager.getTeamList(orgId, searchFilter)) {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(AutoAssignmentAttributes.ID, team.id());
                                jsonObject.put(AutoAssignmentAttributes.NAME, team.getName());
                                teams.put(jsonObject);
                            }
                            crmResponse.put(AutoAssignmentAttributes.TEAMS, teams);
                        }
                    }
                    final int teamAndUsersSize = (int) (usersSize + teamSize);
                    final long territoryCount = territoryManager.getTerritoriesCount(orgId, searchFilter);

                    if (teamAndUsersSize < requiredSize) {
                        final int territoryStart = teamAndUsersSize == 0 ? start : start > teamAndUsersSize ? start - teamAndUsersSize : DEFAULT_START_VALUE;
                        final int territoryLimit = requiredSize - teamAndUsersSize;

                        if (territoryStart <= territoryCount) {
                            searchFilter.setStart(territoryStart);
                            searchFilter.setLimit(territoryLimit);
                            final JsonArray territories = Json.array();

                            for (final Territory territory : territoryManager.getTerritoryList(orgId, searchFilter)) {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(AutoAssignmentAttributes.ID, territory.id());
                                jsonObject.put(AutoAssignmentAttributes.NAME, territory.getName());
                                territories.put(jsonObject);
                            }
                            crmResponse.put(AutoAssignmentAttributes.TERRITORIES, territories);
                        }
                    }
                    final long totalCount = teamAndUsersSize + territoryCount;

                    crmResponse.put(AutoAssignmentAttributes.TOTAL_RECORDS, totalCount);
                    crmResponse.put(AutoAssignmentAttributes.HAS_MORE, requiredSize < totalCount);
                } catch (Exception exception) {
                    LOGGER.error("Auto assignment: Failed to retrieve the users, teams and territories list. Reason: {}", exception.getMessage(), exception);
                    sendError(crmResponse, null, ErrorCode.get(AutoAssignmentErrorCodes.FAILED_TO_RETRIEVE_DATA, "i18n.auto.assignment.user.team.territory.retrieval.failed"));
                } finally {
                    crmResponse.send();
                }
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Cause of the failure.
             */
            @Override
            public void onFailure(final Throwable throwable) {
                crmResponse.setErrorCode(ErrorCode.get(AutoAssignmentErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed"));
                crmResponse.put(AutoAssignmentAttributes.ERRORS, crmResponse.resolveI18nString("i18n.iam.user.retrieval.failed"));
                crmResponse.send();
            }
        };
        userService.fetchUserPage(false, organizationService.getIamOrgId(orgId), searchFilters, usersFetchCallBack);
    }

    /**
     * Dispatches CRM users based on the specified organization and territory details.
     *
     * @param orgId    The unique identifier of the organization.
     * @param data     A {@link JsonObject} containing additional input parameters or payload data.
     * @param response The {@link CrmResponse} object to capture and return the dispatch results.
     */
    private void dispatchUsers(final long orgId, final JsonObject data, final CrmResponse response) {
        final Long ruleTerritoryId = data.getNullableValue(AutoAssignmentAttributes.RULE_TERRITORY_ID);
        final int start = data.optInt(AutoAssignmentAttributes.START, DEFAULT_START_VALUE);
        final int limit = data.optInt(AutoAssignmentAttributes.LIMIT, DEFAULT_LIMIT);
        final TriConsumer<Long, Collection<CrmUser>, Collection<Long>> processUsers = (usersCount, crmUsers, ruleTerritoryUserIds) -> {
            response.convertIntoAsync();

            final Collection<Long> userIds = crmUsers.stream().map(CrmUser::id).toList();
            final EventCallback<Collection<User>> usersFetchCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final Collection<User> users) {
                    try {
                        final Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::id, Function.identity()));
                        final JsonArray ruleTerritoryUsers = Json.array();
                        final JsonArray nonRuleTerritoryUsers = Json.array();

                        for (final CrmUser crmUser : crmUsers) {
                            final JsonObject jsonObject = Json.object();
                            final JsonObject configJson = Json.object();
                            final User user = userMap.get(crmUser.getIamId());

                            configJson.put(AutoAssignmentAttributes.IS_ACTIVE, Objects.equals(User.State.ACTIVE, user.getUserState()));

                            jsonObject.put(AutoAssignmentAttributes.ID, crmUser.id());
                            jsonObject.put(AutoAssignmentAttributes.NAME, crmUser.getFullName());
                            jsonObject.put(AutoAssignmentAttributes.CONFIG, configJson);

                            if (ruleTerritoryUserIds.contains(crmUser.id())) {
                                ruleTerritoryUsers.put(jsonObject);
                            } else {
                                nonRuleTerritoryUsers.put(jsonObject);
                            }
                        }

                        if (nonRuleTerritoryUsers.size() > 0) {
                            response.put(AutoAssignmentAttributes.NON_TERRITORY_USERS, nonRuleTerritoryUsers);
                        }

                        if (ruleTerritoryUsers.size() > 0) {
                            response.put(AutoAssignmentAttributes.TERRITORY_USERS, ruleTerritoryUsers);
                        }

                        response.put(AutoAssignmentAttributes.TOTAL_RECORDS, usersCount);
                        response.put(AutoAssignmentAttributes.HAS_MORE, (start + limit) <= usersCount);
                    } catch (Exception exception) {
                        response.setErrorCode(ErrorCode.get(AutoAssignmentErrorCodes.FAILED_TO_RETRIEVE_DATA, "i18n.auto.assignment.retrieval.failed", exception));
                    } finally {
                        response.send();
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    response.setErrorCode(ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                    response.send();
                }
            };
            userService.fetchUsers(getIamOrgId(orgId), getIamUserIds(orgId, userIds), usersFetchCallBack);
        };

        if (data.containsKey(AutoAssignmentAttributes.TERRITORY_ID)) {
            final Collection<Long> ruleTerritoryUserIds = Objects.nonNull(ruleTerritoryId) ? territoryManager.getTerritoryUserIds(orgId, List.of(ruleTerritoryId)) : Collections.emptySet();
            final Long territoryId = data.getLong(AutoAssignmentAttributes.TERRITORY_ID);
            final long usersCount = territoryManager.getUsersCount(orgId, territoryId);
            final Collection<CrmUser> territoryUsers = usersCount > 0 && start <= usersCount
                    ? territoryManager.getUsers(orgId, territoryId, start, limit) : Collections.emptyList();

            processUsers.accept(usersCount, territoryUsers, ruleTerritoryUserIds);
        } else if (data.containsKey(AutoAssignmentAttributes.TEAM_ID)) {
            final Collection<Long> ruleTerritoryUserIds = Objects.nonNull(ruleTerritoryId) ? territoryManager.getTerritoryUserIds(orgId, List.of(ruleTerritoryId)) : Collections.emptySet();
            final Long teamId = data.getLong(AutoAssignmentAttributes.TEAM_ID);
            final long usersCount = teamManager.getUsersCount(orgId, teamId);
            final Collection<CrmUser> teamUsers = usersCount > 0 && start <= usersCount
                    ? teamManager.getUsers(orgId, teamId, false, start, limit) : Collections.emptyList();

            processUsers.accept(usersCount, teamUsers, ruleTerritoryUserIds);
        } else {
            if (Objects.nonNull(ruleTerritoryId)) {
                final long usersCount = territoryManager.getUsersCount(orgId, ruleTerritoryId);
                final Collection<CrmUser> crmUsers = usersCount > 0 && start <= usersCount
                        ? territoryManager.getUsers(orgId, ruleTerritoryId, start, limit) : Collections.emptyList();

                processUsers.accept(usersCount, crmUsers, crmUsers.stream().map(CrmUser::id).toList());
            }
        }
    }

}

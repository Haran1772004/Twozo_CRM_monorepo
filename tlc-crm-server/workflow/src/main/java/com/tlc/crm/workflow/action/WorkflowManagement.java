package com.tlc.crm.workflow.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.PresetConfig.Configuration;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;
import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.Category.Type;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.FieldType;

import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.CriteriaDropdownInfo;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowComparator.Category;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;
import com.tlc.crm.workflow.api.model.converter.WorkflowConverter;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.cache.service.WorkflowTemplateService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.resolver.internal.WorkflowActionResolver;
import com.tlc.crm.workflow.resolver.internal.WorkflowCriteriaResolver;
import com.tlc.crm.workflow.service.WorkflowCriteriaFieldService;

import com.tlc.i18n.I18nResolver;

import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Manages the {@link Workflow} related web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@SettingsAuthorizer(category = Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_WORKFLOWS)
@WebAction(path = "/workflow/mgmt")
public class WorkflowManagement extends CrmConfigAction<Workflow> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowManagement.class);

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 250;
    private static final String MULTI_DROPDOWN = "MULTI_DROPDOWN";

    private final ConverterConfigBuilder converterConfigBuilder;
    private final WorkflowManager workflowManager;
    private final WorkflowConverter workflowConverter;
    private final WorkflowTemplateService templateService;
    private final WorkflowComparatorService comparatorService;
    private final WorkflowCriteriaFieldService workflowCriteriaFieldService;
    private final WorkflowComparatorService workflowComparatorService;
    private final WorkflowActionResolver workflowActionResolver;
    private final WABusinessAccountService waBusinessAccountService;
    private final WABusinessAccountManager waBusinessAccountManager;

    public WorkflowManagement() {
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.workflowConverter = WorkflowConverter.getInstance();
        this.templateService = WorkflowTemplateService.getInstance();
        this.comparatorService = WorkflowComparatorService.getInstance();
        this.workflowCriteriaFieldService = WorkflowCriteriaFieldService.getInstance();
        this.workflowComparatorService = WorkflowComparatorService.getInstance();
        this.workflowActionResolver = WorkflowActionResolver.getInstance();
        this.waBusinessAccountService = WABusinessAccountService.getInstance();
        this.waBusinessAccountManager = WABusinessAccountManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the request data.
     * @param response Refers the response to be sent.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(WorkflowAttributes.DATA);
        final String type = requestJson.getString(WorkflowAttributes.TYPE);
        final long orgId = request.orgId();

        switch (type) {
            case "updateStatus" -> updateWorkflowStatus(orgId, data, response);
            case "criteriaFieldList" -> processCriteriaFields(orgId, data, request, response);
        }
    }

    /**
     * <p>
     * Process the criteria fields into the {@link CrmResponse} based on the provided data
     * </p>
     *
     * @param orgId      Represents the organization ID
     * @param data       Which contains the {@link Collection} of data to write.
     * @param crmRequest Holds the {@link CrmRequest} details
     */
    private void processCriteriaFields(final long orgId, final JsonObject data, final CrmRequest crmRequest,
                                       final CrmResponse crmResponse) {
        final JsonObject triggerJson = data.getJsonObject(WorkflowAttributes.TRIGGER);
        final int start = Math.max(data.optInt(NoteFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE) - 1;
        final int limit = Math.min(data.optInt(NoteFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final I18NCRMResolutionAdapter i18nResolver = crmRequest.getI18NCRMResolutionAdapter();
        final Collection<WorkflowCriteriaField> fields = workflowCriteriaFieldService.getFields(orgId, triggerJson.getShort(WorkflowAttributes.SOURCE));
        final Collection<WorkflowCriteriaField> workflowCriteriaFields = getWorkflowCriteriaFields(start, limit, data, fields, i18nResolver);
        final int totalRecords = fields.size();
        final Map<Long, WorkflowComparator> comparators = comparatorService.getAll();

        if (!workflowCriteriaFields.isEmpty()) {
            final JsonArray criteriaFieldsArray = Json.array();

            for (final WorkflowCriteriaField workflowCriteriaField : workflowCriteriaFields) {
                criteriaFieldsArray.put(convertFieldToJson(triggerJson.getShort(WorkflowAttributes.EVENT), workflowCriteriaField, i18nResolver));
            }
            crmResponse.put(WorkflowAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
        }

        if (!comparators.isEmpty()) {
            final JsonObject comparatorJson = Json.object();

            for (final WorkflowComparator workflowComparator : comparators.values()) {
                comparatorJson.put(workflowComparator.idAsString(), convertComparatorToJson(workflowComparator, i18nResolver));
            }
            crmResponse.put(WorkflowAttributes.COMPARATOR, comparatorJson);
        }
        crmResponse.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
        crmResponse.put(WorkflowAttributes.HAS_MORE, (start + limit) <= totalRecords);
    }

    /**
     * <p>
     * Convert {@link WorkflowComparator} details to {@link JsonObject}
     * </p>
     *
     * @param workflowComparator It holds the {@link WorkflowComparator} details
     * @param i18nResolver       Refers the {@link I18nResolver} for resolve the i18n data
     * @return The converted comparator {@link JsonObject}
     */
    private JsonObject convertComparatorToJson(final WorkflowComparator workflowComparator, final I18NCRMResolutionAdapter i18nResolver) {
        return Json.object().put(WorkflowAttributes.NAME, i18nResolver.get(workflowComparator.getName()))
                .put(WorkflowAttributes.HAS_VALUE, workflowComparator.hasValue());
    }

    /**
     * <p>
     * Retrieves the filtered {@link Collection} of {@link WorkflowCriteriaField} based on specified criteria, with an limitation applied by the start,limit
     * </p>
     *
     * @param start                  Represents the starting index of criteria fields to be fetched
     * @param limit                  Represents the maximum number of criteria fields to be fetched
     * @param data                   Represents the {@link DropdownRequest}
     * @param workflowCriteriaFields Represents the {@link Collection} of {@link WorkflowCriteriaField}
     * @return filtered {@link Collection} of {@link WorkflowCriteriaField}
     */
    private Collection<WorkflowCriteriaField> getWorkflowCriteriaFields(final int start, final int limit, final JsonObject data,
                                                                        final Collection<WorkflowCriteriaField> workflowCriteriaFields,
                                                                        final I18NCRMResolutionAdapter i18nResolver) {
        final DropdownCriteria dropdownCriteria = new DropdownCriteria();

        if (data.containsKey(WorkflowAttributes.CRITERIA)) {
            final JsonObject criteriaJson = data.getJsonObject(WorkflowAttributes.CRITERIA);

            dropdownCriteria.setCriteriaType(criteriaJson.getString(WorkflowAttributes.TYPE));
            dropdownCriteria.setCriteriaValue(criteriaJson.getString(WorkflowAttributes.VALUE));
        }

        if (dropdownCriteria.getValue() instanceof final String criteriaFieldValue) {
            final String name = criteriaFieldValue.toUpperCase();

            return workflowCriteriaFields.stream().filter(workflowCriteriaField -> {
                final String fieldName = i18nResolver.get(workflowCriteriaField.getName()).toUpperCase();

                return switch (dropdownCriteria.getCriteriaType()) {
                    case STARTS_WITH -> fieldName.startsWith(name);
                    case CONTAINS -> fieldName.contains(name);
                    default -> fieldName.equalsIgnoreCase(criteriaFieldValue);
                };
            }).skip(start).limit(limit).toList();
        } else {
            return workflowCriteriaFields.stream().skip(start).limit(limit).toList();
        }
    }

    /**
     * <p>
     * Converts the {@link WorkflowCriteriaField} to {@link JsonObject}
     * </p>
     *
     * @param triggerEventId        Represents the trigger event ID
     * @param workflowCriteriaField Represents the {@link WorkflowCriteriaField}
     * @param i18nResolver          Represents the {@link I18NCRMResolutionAdapter}
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldToJson(final short triggerEventId, final WorkflowCriteriaField workflowCriteriaField, final I18NCRMResolutionAdapter i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final FieldType fieldType = workflowCriteriaField.getFieldType();
        final CriteriaDropdownInfo criteriaDropdownInfo = workflowCriteriaField.getCriteriaDropdownInfo();
        final Collection<WorkflowComparator> comparators = workflowComparatorService.getComparatorsByFieldTypeId(fieldType.id());
        final Collection<Long> comparatorIds;
        final TriggerEvent triggerEvent = TriggerEvent.get(triggerEventId);

        if (TriggerEvent.CREATED == triggerEvent || TriggerEvent.DELETED == triggerEvent) {
            comparatorIds = comparators.stream().filter(comparator -> Category.UPDATE != comparator.getCategory())
                    .map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
        } else {
            comparatorIds = comparators.stream().map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
        }

        if (Objects.nonNull(criteriaDropdownInfo)) {
            final JsonObject criteriaDropdownJson = Json.object();

            criteriaDropdownJson.put(WorkflowAttributes.NAME, criteriaDropdownInfo.getDropdownName())
                    .put(WorkflowAttributes.REQUIRED_KEYS, criteriaDropdownInfo.getRequiredKeys());
            jsonObject.put(WorkflowAttributes.DROPDOWN, criteriaDropdownJson);
        }

        jsonObject.put(WorkflowAttributes.FIELD_ID, workflowCriteriaField.getFieldId())
                .put(WorkflowAttributes.NAME, i18nResolver.get(workflowCriteriaField.getName()))
                .put(WorkflowAttributes.FIELD_TYPE, getFieldTypeName(workflowCriteriaField)).put(WorkflowAttributes.COMPARATORS, comparatorIds);
        final JsonObject validationConfig = getValidationConfig(workflowCriteriaField, i18nResolver);

        if (0 < validationConfig.size()) {
            jsonObject.put(WorkflowAttributes.CONFIG, validationConfig);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the validation config of the {@link WorkflowCriteriaField}
     * </p>
     *
     * @param workflowCriteriaField Represents the workflow criteria field
     * @param i18nResolver          Refers the {@code I18nResolver} to resolve the i18n keys.
     * @return the json which contains the validation config
     */
    private JsonObject getValidationConfig(final WorkflowCriteriaField workflowCriteriaField, final I18NCRMResolutionAdapter i18nResolver) {
        final JsonObject fieldConfig = workflowCriteriaField.getFieldConfig();
        final JsonObject validationConfig = Json.object();
        final String maxKey = Configuration.MAX.getName();
        final String minKey = Configuration.MIN.getName();
        final String minLengthKey = Configuration.MIN_LENGTH.getName();
        final String maxLengthKey = Configuration.MAX_LENGTH.getName();
        final String maxLimitKey = Configuration.MAX_LIMIT.getName();
        final String validationKey = Configuration.PATTERN.getName();

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
                validationJson.put(violationMessageKey, i18nResolver.get(violationMessage));
                validationJson.remove(messageKey);
            }

            if (validationJson.containsKey(valueKey)) {
                validationJson.put(FieldKeys.REGEX.getName(), validationJson.get(valueKey));
                validationJson.remove(valueKey);
            }
            validationConfig.put(Configuration.VALIDATION.getName(), validationJson);
        }

        return validationConfig;
    }

    /**
     * Gets the field type name of the {@link WorkflowCriteriaField}.
     *
     * @param workflowCriteriaField Refers the {@link WorkflowCriteriaField}.
     * @return The field type name.
     */
    private String getFieldTypeName(final WorkflowCriteriaField workflowCriteriaField) {
        final Optional<String> optionalAliasName = Optional.ofNullable(workflowCriteriaField.getAliasName());
        final String fieldType = workflowCriteriaField.getFieldType().name();

        return optionalAliasName.map(aliasName -> switch (aliasName) {
            case "pipelineStage" -> MULTI_DROPDOWN;
            case "title" -> StoreType.SINGLE_STRING.getName();
            case "products", "deals", "closedDeals", "wonDeals", "lostDeals" -> StoreType.SINGLE_NUMBER.getName();
            default -> fieldType;
        }).orElseGet(() -> fieldType);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link WorkflowManager} instance.
     */
    @Override
    public ConfigManager<Workflow> getConfigManager() {
        return workflowManager;
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Refers the request data.
     * @param jsonObject Holds entity details.
     * @return {@link Workflow} details.
     */
    @Override
    public Workflow convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return workflowConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request    Refers the request data.
     * @param jsonObject Holds entity details.
     * @return {@link Workflow} details.
     */
    @Override
    public Workflow convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return workflowConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param request  The received request.
     * @param workflow Refers the {@link Workflow}.
     * @return The {@link JsonObject} of workflow data.
     */
    @Override
    public JsonObject convert(final CrmRequest request, final Workflow workflow) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return workflowConverter.convert(converterConfig, workflow);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organization id.
     * @param userId    Represents the user id.
     * @param action    Holds the action values.
     * @param workflows Collection of {@link Workflow}.
     * @return true if all details are valid.
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<Workflow> workflows) {
        switch (action) {
            case CREATE, UPDATE -> {
                final Set<Long> templateIds = new HashSet<>();
                final Set<Long> comparatorIds = new HashSet<>();

                for (final Workflow workflow : workflows) {
                    final Collection<WorkflowCriteriaSet> criteriaSets = workflow.getCriteriaSets();
                    final Long templateId = workflow.getTemplateId();

                    if (Objects.nonNull(templateId)) {
                        templateIds.add(templateId);
                    }

                    if (Objects.nonNull(criteriaSets)) {

                        for (final WorkflowCriteriaSet workflowCriteriaSet : criteriaSets) {
                            comparatorIds.addAll(workflowCriteriaSet.getCriteriaList().stream()
                                    .map(criteria -> criteria.getComparator().getId()).collect(Collectors.toUnmodifiableSet()));
                        }
                    }
                }

                if (!templateIds.isEmpty() && !templateService.exists(templateIds)) {
                    throw ErrorCode.get(WorkflowErrorCodes.TEMPLATE_NOT_FOUND, "i18n.workflow.validation.invalid.template");
                }

                if (!comparatorIds.isEmpty() && !comparatorService.exists(comparatorIds)) {
                    throw ErrorCode.get(WorkflowErrorCodes.COMPARATORS_NOT_FOUND, "i18n.workflow.validation.invalid.comparator");
                }
            }
        }

        return true;
    }

    /**
     * Updates the status of the {@link Workflow}.
     *
     * @param orgId    Refers the organization id.
     * @param data     Refers the request json data.
     * @param response Refers the response to be sent.
     */
    private void updateWorkflowStatus(final long orgId, final JsonObject data, final CrmResponse response) {
        final long id = data.getLong(WorkflowAttributes.ID);

        if (workflowManager.exists(orgId, id)) {
            workflowManager.updateStatus(orgId, id, data.getBoolean(WorkflowAttributes.IS_ACTIVE));
        } else {
            throw ErrorCode.get(WorkflowErrorCodes.WORKFLOW_NOT_FOUND, "i18n.workflow.not.found");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Holds the {@link CrmRequest} details.
     * @param workflow    Refers the {@link Workflow} details.
     * @param crmResponse Refers the {@link CrmResponse} to be sent.
     */
    @Override
    protected final void fetchAndDispatchUser(final CrmRequest request, final Workflow workflow, final CrmResponse crmResponse) {
        final Long orgId = workflow.orgId();
        final JsonObject jsonObject = convert(request, workflow);
        final Collection<Long> userIds = workflow.getUserIds();

        if (!userIds.isEmpty()) {
            final Map<Long, CrmUser> userMap = crmUserService.getUsers(orgId, userIds).stream().collect(Collectors.toMap(CrmUser::id, Function.identity()));

            if (jsonObject.containsKey(WorkflowAttributes.CONDITIONS)) {
                final JsonArray conditionArray = jsonObject.getJsonArray(WorkflowAttributes.CONDITIONS);

                WorkflowCriteriaResolver.getInstance().resolveUserConditions(orgId, workflow.getTriggerSource(), conditionArray, userMap);
            }
            workflowActionResolver.resolveUserActionFieldValues(orgId, jsonObject.getJsonArray(WorkflowAttributes.ACTIONS), userMap);
        }
        final boolean isWhatsAppActionPresent = workflow.getActions().stream().anyMatch(action -> Objects.equals(WorkflowAttributes.WHATSAPP, action.getTriggerActionSource().getAliasName()));

        if (isWhatsAppActionPresent) {
            loadWhatsappFieldValuesAndDispatch(orgId, jsonObject, crmResponse);
        } else {
            jsonObject.forEach(crmResponse::put);
        }
    }

    /**
     * <p>
     * Loads the whatsapp field values for the actions and dispatches the response.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param workflowJson Represents the workflow details in json format.
     * @param crmResponse  Represents the response to be sent.
     */
    private void loadWhatsappFieldValuesAndDispatch(final Long orgId, final JsonObject workflowJson, final CrmResponse crmResponse) {
        final Collection<Long> businessPhoneNumberIds = new HashSet<>(NumericalConstant.FIVE);
        final Collection<Long> templateIds = new HashSet<>(NumericalConstant.FIVE);
        final JsonArray actions = workflowJson.getJsonArray(WorkflowAttributes.ACTIONS);
        final int actionsSize = actions.size();

        for (int index = 0; index < actionsSize; index++) {
            final JsonObject actionJson = actions.getJsonObject(index);
            final JsonObject sourceJson = actionJson.getJsonObject(WorkflowAttributes.SOURCE);

            if (WorkflowAttributes.WHATSAPP.equalsIgnoreCase(sourceJson.getString(WorkflowAttributes.NAME))) {
                final JsonObject fieldMapping = actionJson.getJsonObject(WorkflowAttributes.FIELD_MAPPING);
                final JsonObject templateValue = fieldMapping.getJsonObject(WorkflowAttributes.TEMPLATE)
                        .getJsonArray(WorkflowAttributes.VALUES).getJsonObject(NumericalConstant.ZERO);
                final JsonObject businessPhoneNumberValue = fieldMapping.getJsonObject(WorkflowAttributes.SENDER_WHATSAPP_NUMBER)
                        .getJsonArray(WorkflowAttributes.VALUES).getJsonObject(NumericalConstant.ZERO);

                templateIds.add(templateValue.getJsonObject(WorkflowAttributes.VALUE).getLong(WorkflowAttributes.TEMPLATE_ID));
                businessPhoneNumberIds.add(businessPhoneNumberValue.getLong(WorkflowAttributes.VALUE));
            }
        }

        if (!templateIds.isEmpty() && !businessPhoneNumberIds.isEmpty()) {
            crmResponse.convertIntoAsync();
            final Map<Long, Long> phoneNumberIdMap = waBusinessAccountManager.getPhoneNumberIds(orgId, businessPhoneNumberIds);

            final EventCallback<Map<Long, WABusinessPhoneNumber>> callback = new EventCallback<>() {

                @Override
                public void onSuccess(final Map<Long, WABusinessPhoneNumber> phoneNumberMap) {

                    final EventCallback<Map<Long, String>> callback = new EventCallback<>() {

                        @Override
                        public void onSuccess(final Map<Long, String> templateMap) {
                            try {
                                for (int index = 0; index < actionsSize; index++) {
                                    final JsonObject actionJson = actions.getJsonObject(index);
                                    final JsonObject sourceJson = actionJson.getJsonObject(WorkflowAttributes.SOURCE);

                                    if (WorkflowAttributes.WHATSAPP.equalsIgnoreCase(sourceJson.getString(WorkflowAttributes.NAME))) {
                                        final JsonObject fieldMapping = actionJson.getJsonObject(WorkflowAttributes.FIELD_MAPPING);
                                        final JsonObject existingTemplateValue = fieldMapping.getJsonObject(WorkflowAttributes.TEMPLATE)
                                                .getJsonArray(WorkflowAttributes.VALUES).getJsonObject(NumericalConstant.ZERO).getJsonObject(WorkflowAttributes.VALUE);

                                        existingTemplateValue.put(WorkflowAttributes.TEMPLATE_NAME, templateMap.get(existingTemplateValue.getLong(WorkflowAttributes.TEMPLATE_ID)));

                                        final JsonObject existingBusinessPhoneNumberValue = fieldMapping.getJsonObject(WorkflowAttributes.SENDER_WHATSAPP_NUMBER)
                                                .getJsonArray(WorkflowAttributes.VALUES).getJsonObject(NumericalConstant.ZERO);
                                        final Long senderPhoneNumberId = existingBusinessPhoneNumberValue.getLong(WorkflowAttributes.VALUE);
                                        final JsonObject phoneNumberJson = Json.object();

                                        phoneNumberJson.put(WorkflowAttributes.ID, senderPhoneNumberId);
                                        phoneNumberJson.put(WorkflowAttributes.NAME, phoneNumberMap.get(phoneNumberIdMap.get(senderPhoneNumberId)).getDisplayPhoneNumber());

                                        existingBusinessPhoneNumberValue.put(WorkflowAttributes.VALUE, phoneNumberJson);
                                    }
                                }
                                workflowJson.forEach(crmResponse::put);
                            } catch (Throwable throwable) {
                                final String errorDetail = throwable.getMessage();

                                LoggerUtil.logError(LOGGER, throwable, "Whatsapp: Template retrieved but error during conversion. OrgId: {}, Reason: {}", orgId, errorDetail);
                                setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"), errorDetail, null);
                            } finally {
                                crmResponse.send();
                            }
                        }

                        @Override
                        public void onFailure(Throwable cause) {
                            final String errorDetail = cause.getMessage();

                            LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to retrieve template. OrgId: {}, Reason: {}", orgId, errorDetail);
                            setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.TEMPLATE_RETRIEVAL_FAILED, "i18n.whatsapp.template.retrieval.failed"), errorDetail, null);
                            crmResponse.send();
                        }
                    };

                    waBusinessAccountService.fetchTemplateNames(templateIds, callback);
                }

                @Override
                public void onFailure(final Throwable cause) {
                    final String errorDetail = cause.getMessage();

                    LoggerUtil.logError(LOGGER, cause, "Whatsapp: Failed to fetch whatsapp phone numbers, OrgId: {}, Phone number Ids: {}, Message: {}", orgId, businessPhoneNumberIds, errorDetail);
                    setErrorDescriptor(crmResponse, ErrorCode.get(WAErrorCodes.FAILED_TO_FETCH_BUSINESS_PHONE_NUMBERS, "i18n.whatsapp.business.phone.numbers.retrieval.failed"), errorDetail, null);
                    crmResponse.send();
                }
            };

            waBusinessAccountService.fetchBusinessPhoneNumbers(phoneNumberIdMap.values(), callback);
        } else {
            workflowJson.forEach(crmResponse::put);
        }
    }
}

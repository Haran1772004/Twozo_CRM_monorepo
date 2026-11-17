package com.tlc.crm.workflow.v2.action;

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
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;

import com.tlc.crm.core.field.contact.ContactFields;
import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.features.field.note.NoteFields;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.PresetConfig;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.messenger.internal.status.whatsapp.WAErrorCodes;

import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.crm.security.permission.model.Category.Type;

import com.tlc.crm.workflow.api.model.CriteriaDropdownInfo;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.api.model.WorkflowCriteriaField;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.cache.service.WorkflowTemplateService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.service.WorkflowCriteriaFieldService;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.PredefinedSpanUnit;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.model.converter.WorkflowV2Converter;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

import com.tlc.i18n.I18nResolver;

import com.tlc.social.client.service.whatsapp.WABusinessAccountService;
import com.tlc.social.common.whatsapp.model.WABusinessPhoneNumber;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import com.tlc.web.WebAction;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Manages workflow V2 definition CRUD web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
@SettingsAuthorizer(category = Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_WORKFLOWS)
@WebAction(path = "/v2/workflow")
public class WorkflowV2Management extends CrmConfigAction<WorkflowV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2Management.class);

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 250;
    private static final String MULTI_DROPDOWN = "MULTI_DROPDOWN";

    private final ConverterConfigBuilder converterConfigBuilder;
    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowV2Converter workflowV2Converter;
    private final WorkflowTemplateService templateService;
    private final WorkflowComparatorService comparatorService;
    private final WorkflowCriteriaFieldService workflowCriteriaFieldService;
    private final WABusinessAccountService waBusinessAccountService;
    private final WABusinessAccountManager waBusinessAccountManager;
    private final TriggerActionEventService triggerActionEventService;
    private final TriggerActionSourceService triggerActionSourceService;

    public WorkflowV2Management() {
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.workflowV2Converter = WorkflowV2Converter.getInstance();
        this.templateService = WorkflowTemplateService.getInstance();
        this.comparatorService = WorkflowComparatorService.getInstance();
        this.workflowCriteriaFieldService = WorkflowCriteriaFieldService.getInstance();
        this.waBusinessAccountService = WABusinessAccountService.getInstance();
        this.waBusinessAccountManager = WABusinessAccountManager.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the CrmRequest.
     * @param response Refers the CrmResponse.
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(WorkflowAttributesV2.DATA);
        final String type = requestJson.getString(WorkflowAttributesV2.TYPE);
        final long orgId = request.orgId();

        switch (type) {
            case "updateStatus" -> updateWorkflowStatus(orgId, data);
            case "criteriaFieldList" -> processCriteriaFields(orgId, data, request, response);
            case "delayDurationList" -> response.put(WorkflowAttributesV2.LIST, getDurationList());
            case "getCriteriaEvents" ->
                    response.put(WorkflowAttributesV2.LIST, getCriteriaEvents(orgId, data, request));
            case "criteriaFields" -> processCriteriaFieldList(orgId, data, request, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<WorkflowV2> getConfigManager() {
        return workflowV2Manager;
    }

    /**
     * Gets the list of criteria events for a workflow.
     *
     * @param orgId      The organization ID.
     * @param data       The request data.
     * @param crmRequest The CRM request.
     * @return The list of criteria events.
     */
    private JsonArray getCriteriaEvents(final long orgId, final JsonObject data, final CrmRequest crmRequest) {
        final JsonArray jsonArray = Json.array();
        final JsonObject triggerJson = data.getJsonObject(WorkflowAttributes.TRIGGER);
        final NodeType nodeType = NodeType.get(data.getShort(WorkflowAttributesV2.NODE_TYPE));
        final I18NCRMResolutionAdapter i18nResolver = crmRequest.getI18NCRMResolutionAdapter();
        final TriggerEvent event = TriggerEvent.get(triggerJson.getShort(WorkflowAttributes.EVENT));
        final short triggerSource = triggerJson.getShort(WorkflowAttributes.SOURCE);

        if (TriggerEvent.DELETED != event) {
            final TriggerEvent triggerEvent = NodeType.WAIT_FOR_CONDITION == nodeType ? TriggerEvent.EDITED : event;
            final JsonObject eventJson = Json.object();

            eventJson.put(WorkflowAttributesV2.TYPE, ActionAttributes.TRIGGER);
            eventJson.put(WorkflowAttributesV2.SOURCE, triggerSource);
            eventJson.put(WorkflowAttributesV2.NODE, triggerJson.getShort(WorkflowAttributesV2.NODE));
            eventJson.put(WorkflowAttributesV2.EVENT, event.getId());
            eventJson.put(WorkflowAttributes.NAME, String.format("%s %s", i18nResolver.get(TriggerSource.get(triggerSource).getI18nKey()), triggerEvent.getName()));

            jsonArray.put(eventJson);
        }

        final JsonArray actionsArray = data.getJsonArray(WorkflowAttributes.ACTIONS);
        final int size = actionsArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject actionJson = actionsArray.getJsonObject(index);
            final long actionSourceId = actionJson.getLong(WorkflowAttributes.SOURCE);
            final TriggerActionEvent triggerActionEvent = triggerActionEventService.get(actionJson.getLong(WorkflowAttributes.EVENT));
            final JsonObject jsonObject = Json.object();
            final TriggerActionSource triggerActionSource = triggerActionSourceService.get(actionSourceId);
            final boolean isUpdateEvent = NodeType.WAIT_FOR_CONDITION == nodeType || triggerActionEvent.isUpdateEvent();

            jsonObject.put(WorkflowAttributesV2.TYPE, ActionAttributes.ACTION);
            jsonObject.put(WorkflowAttributesV2.SOURCE, triggerActionSource.id());
            jsonObject.put(WorkflowAttributesV2.NODE, actionJson.getShort(WorkflowAttributesV2.NODE));
            jsonObject.put(WorkflowAttributesV2.EVENT, triggerActionEvent.id());
            jsonObject.put(WorkflowAttributes.NAME, String.format("%s %s", i18nResolver.get(triggerActionSource.getDisplayName()),
                    isUpdateEvent ? WorkflowAttributesV2.UPDATED : WorkflowAttributesV2.CREATED));

            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /**
     * Processes the criteria field list for a workflow.
     *
     * @param orgId    The organization ID.
     * @param data     The request data.
     * @param request  The CRM request.
     * @param response The CRM response.
     */
    private void processCriteriaFieldList(final long orgId, final JsonObject data,
                                          final CrmRequest request, final CrmResponse response) {
        final NodeType nodeType = NodeType.get(data.getShort(WorkflowAttributesV2.NODE_TYPE));
        final I18NCRMResolutionAdapter i18nResolver = request.getI18NCRMResolutionAdapter();
        final int start = Math.max(data.optInt(NoteFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE) - 1;
        final int limit = Math.min(data.optInt(NoteFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);

        switch (data.getString(WorkflowAttributesV2.TYPE).toUpperCase()) {
            case ActionAttributes.TRIGGER -> {
                final TriggerEvent event = TriggerEvent.get(data.getShort(WorkflowAttributes.EVENT));
                final short triggerSource = data.getShort(WorkflowAttributes.SOURCE);
                final Collection<WorkflowCriteriaField> triggerFields = NodeType.WAIT_FOR_CONDITION == nodeType && TriggerEvent.DELETED == event
                        ? Collections.emptyList() : workflowCriteriaFieldService.getFields(orgId, triggerSource);
                final int totalRecords = triggerFields.size();

                if (!triggerFields.isEmpty()) {
                    final Collection<WorkflowCriteriaField> workflowCriteriaFields = getWorkflowCriteriaFields(start, limit, data, triggerFields, i18nResolver);
                    final JsonArray criteriaFieldsArray = Json.array();
                    final TriggerEvent triggerEvent = NodeType.WAIT_FOR_CONDITION == nodeType ? TriggerEvent.EDITED : event;

                    for (final WorkflowCriteriaField workflowCriteriaField : workflowCriteriaFields) {
                        final Collection<WorkflowComparator> comparators = comparatorService.getComparatorsByFieldTypeId(workflowCriteriaField.getFieldType().id());
                        final Collection<Long> comparatorIds;

                        if (TriggerSource.TriggerEvent.CREATED == triggerEvent || TriggerSource.TriggerEvent.DELETED == triggerEvent) {
                            comparatorIds = comparators.stream().filter(comparator -> WorkflowComparator.Category.UPDATE != comparator.getCategory())
                                    .map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                        } else {
                            comparatorIds = comparators.stream().map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                        }
                        criteriaFieldsArray.put(convertFieldToJson(workflowCriteriaField, comparatorIds, i18nResolver));
                    }
                    response.put(WorkflowAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
                    response.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
                    response.put(WorkflowAttributes.HAS_MORE, (start + limit) <= totalRecords);
                }
            }
            case ActionAttributes.ACTION -> {
                final long actionSourceId = data.getLong(WorkflowAttributes.SOURCE);
                final TriggerActionEvent triggerActionEvent = triggerActionEventService.get(data.getLong(WorkflowAttributes.EVENT));
                final Collection<WorkflowCriteriaField> actionFields = triggerActionEvent.isCommonActionEvent() ?
                        workflowCriteriaFieldService.getFields(orgId, actionSourceId) : Collections.emptyList();
                final int totalRecords = actionFields.size();

                if (!actionFields.isEmpty()) {
                    final Collection<WorkflowCriteriaField> workflowCriteriaFields = getWorkflowCriteriaFields(start, limit, data, actionFields, i18nResolver);
                    final JsonArray criteriaFieldsArray = Json.array();
                    final boolean isUpdateEvent = NodeType.WAIT_FOR_CONDITION == nodeType || triggerActionEvent.isUpdateEvent();

                    for (final WorkflowCriteriaField workflowCriteriaField : workflowCriteriaFields) {
                        final Collection<WorkflowComparator> comparators = comparatorService.getComparatorsByFieldTypeId(workflowCriteriaField.getFieldType().id());
                        final Collection<Long> comparatorIds;

                        if (isUpdateEvent) {
                            comparatorIds = comparators.stream().map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                        } else {
                            comparatorIds = comparators.stream().filter(comparator -> WorkflowComparator.Category.UPDATE != comparator.getCategory())
                                    .map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                        }
                        criteriaFieldsArray.put(convertFieldToJson(workflowCriteriaField, comparatorIds, i18nResolver));
                    }

                    response.put(WorkflowAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
                    response.put(WorkflowAttributes.TOTAL_RECORDS, totalRecords);
                    response.put(WorkflowAttributes.HAS_MORE, (start + limit) <= totalRecords);
                }
            }
        }
        final Map<Long, WorkflowComparator> comparators = comparatorService.getAll();

        if (!comparators.isEmpty()) {
            final JsonObject comparatorJson = Json.object();

            for (final WorkflowComparator workflowComparator : comparators.values()) {
                comparatorJson.put(workflowComparator.idAsString(), convertComparatorToJson(workflowComparator, i18nResolver));
            }
            response.put(WorkflowAttributes.COMPARATOR, comparatorJson);
        }
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
     * Processes the criteria fields for a workflow.
     *
     * @param orgId       the organization id
     * @param data        the data
     * @param crmRequest  the CRM request
     * @param crmResponse the CRM response
     */
    private void processCriteriaFields(final long orgId, final JsonObject data, final CrmRequest crmRequest,
                                       final CrmResponse crmResponse) {
        final JsonArray jsonArray = Json.array();
        final JsonObject triggerJson = data.getJsonObject(WorkflowAttributes.TRIGGER);
        final NodeType nodeType = NodeType.get(data.getShort(WorkflowAttributesV2.NODE_TYPE));
        final I18NCRMResolutionAdapter i18nResolver = crmRequest.getI18NCRMResolutionAdapter();
        final TriggerEvent event = TriggerEvent.get(triggerJson.getShort(WorkflowAttributes.EVENT));
        final short triggerSource = triggerJson.getShort(WorkflowAttributes.SOURCE);
        final Collection<WorkflowCriteriaField> triggerFields = NodeType.WAIT_FOR_CONDITION == nodeType && TriggerEvent.DELETED == event
                ? Collections.emptyList() : workflowCriteriaFieldService.getFields(orgId, triggerSource);

        if (!triggerFields.isEmpty()) {
            final JsonObject jsonObject = Json.object();
            final JsonArray criteriaFieldsArray = Json.array();
            final TriggerEvent triggerEvent = NodeType.WAIT_FOR_CONDITION == nodeType ? TriggerEvent.EDITED : event;

            for (final WorkflowCriteriaField workflowCriteriaField : triggerFields) {
                final Collection<WorkflowComparator> comparators = comparatorService.getComparatorsByFieldTypeId(workflowCriteriaField.getFieldType().id());
                final Collection<Long> comparatorIds;

                if (TriggerSource.TriggerEvent.CREATED == triggerEvent || TriggerSource.TriggerEvent.DELETED == triggerEvent) {
                    comparatorIds = comparators.stream().filter(comparator -> WorkflowComparator.Category.UPDATE != comparator.getCategory())
                            .map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                } else {
                    comparatorIds = comparators.stream().map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                }
                criteriaFieldsArray.put(convertFieldToJson(workflowCriteriaField, comparatorIds, i18nResolver));
            }

            jsonObject.put(WorkflowAttributes.NAME, String.format("%s %s", i18nResolver.get(TriggerSource.get(triggerSource).getI18nKey()), triggerEvent.getName()));
            jsonObject.put(WorkflowAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
            jsonArray.put(jsonObject);
        }

        final JsonArray actionsArray = data.getJsonArray(WorkflowAttributes.ACTIONS);
        final int size = actionsArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject actionJson = actionsArray.getJsonObject(index);
            final long actionSourceId = actionJson.getLong(WorkflowAttributes.SOURCE);
            final TriggerActionEvent triggerActionEvent = triggerActionEventService.get(actionJson.getLong(WorkflowAttributes.EVENT));
            final Collection<WorkflowCriteriaField> actionFields = triggerActionEvent.isCommonActionEvent() ?
                    workflowCriteriaFieldService.getFields(orgId, actionSourceId) : Collections.emptyList();

            if (!actionFields.isEmpty()) {
                final JsonObject jsonObject = Json.object();
                final JsonArray criteriaFieldsArray = Json.array();
                final TriggerActionSource triggerActionSource = triggerActionSourceService.get(actionSourceId);
                final boolean isUpdateEvent = NodeType.WAIT_FOR_CONDITION == nodeType || triggerActionEvent.isUpdateEvent();

                for (final WorkflowCriteriaField workflowCriteriaField : actionFields) {
                    final Collection<WorkflowComparator> comparators = comparatorService.getComparatorsByFieldTypeId(workflowCriteriaField.getFieldType().id());
                    final Collection<Long> comparatorIds;

                    if (isUpdateEvent) {
                        comparatorIds = comparators.stream().filter(comparator -> WorkflowComparator.Category.UPDATE != comparator.getCategory())
                                .map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                    } else {
                        comparatorIds = comparators.stream().map(WorkflowComparator::getId).sorted(Comparator.naturalOrder()).toList();
                    }
                    criteriaFieldsArray.put(convertFieldToJson(workflowCriteriaField, comparatorIds, i18nResolver));
                }

                jsonObject.put(WorkflowAttributes.NAME, String.format("%s %s", i18nResolver.get(triggerActionSource.getDisplayName()),
                        isUpdateEvent ? WorkflowAttributesV2.UPDATED : WorkflowAttributesV2.CREATED));
                jsonObject.put(WorkflowAttributes.CRITERIA_FIELDS, criteriaFieldsArray);
                jsonArray.put(jsonObject);
            }
        }
        crmResponse.put(WorkflowAttributesV2.LIST, jsonArray);

        final Map<Long, WorkflowComparator> comparators = comparatorService.getAll();

        if (!comparators.isEmpty()) {
            final JsonObject comparatorJson = Json.object();

            for (final WorkflowComparator workflowComparator : comparators.values()) {
                comparatorJson.put(workflowComparator.idAsString(), convertComparatorToJson(workflowComparator, i18nResolver));
            }
            crmResponse.put(WorkflowAttributes.COMPARATOR, comparatorJson);
        }
    }

    /**
     * Converts the {@link WorkflowComparator} details to {@link JsonObject}.
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
     * Converts the {@link WorkflowCriteriaField} to {@link JsonObject}.
     *
     * @param workflowCriteriaField Represents the {@link WorkflowCriteriaField}
     * @param i18nResolver          Represents the {@link I18NCRMResolutionAdapter}
     * @return {@link JsonObject}
     */
    private JsonObject convertFieldToJson(final WorkflowCriteriaField workflowCriteriaField, final Collection<Long> comparatorIds, final I18NCRMResolutionAdapter i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final CriteriaDropdownInfo criteriaDropdownInfo = workflowCriteriaField.getCriteriaDropdownInfo();

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
     * Gets the validation config of the {@link WorkflowCriteriaField}.
     *
     * @param workflowCriteriaField Represents the workflow criteria field
     * @param i18nResolver          Refers the {@code I18nResolver} to resolve the i18n keys.
     * @return the json which contains the validation config
     */
    private JsonObject getValidationConfig(final WorkflowCriteriaField workflowCriteriaField, final I18NCRMResolutionAdapter i18nResolver) {
        final JsonObject fieldConfig = workflowCriteriaField.getFieldConfig();
        final JsonObject validationConfig = Json.object();
        final String maxKey = PresetConfig.Configuration.MAX.getName();
        final String minKey = PresetConfig.Configuration.MIN.getName();
        final String minLengthKey = PresetConfig.Configuration.MIN_LENGTH.getName();
        final String maxLengthKey = PresetConfig.Configuration.MAX_LENGTH.getName();
        final String maxLimitKey = PresetConfig.Configuration.MAX_LIMIT.getName();
        final String validationKey = PresetConfig.Configuration.PATTERN.getName();
        final String options = FieldKeys.OPTIONS.getName();

        if (FieldGroup.WORKFLOW_RELATED_FIELD == workflowCriteriaField.getFieldGroup()
                && fieldConfig.containsKey(options)) {
            validationConfig.put(options, fieldConfig.getJsonArray(options));
        }

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
            validationConfig.put(PresetConfig.Configuration.VALIDATION.getName(), validationJson);
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
            case DealFields.PIPELINE_STAGE -> MULTI_DROPDOWN;
            case DealFields.TITLE -> StoreType.SINGLE_STRING.getName();
            case DealFields.PRODUCTS, DealFields.DEALS, ContactFields.CLOSED_DEALS, ContactFields.WON_DEALS,
                 ContactFields.LOST_DEALS -> StoreType.SINGLE_NUMBER.getName();
            default -> fieldType;
        }).orElseGet(() -> fieldType);
    }

    /**
     * Gets the duration list.
     *
     * @return the duration list
     */
    private JsonArray getDurationList() {
        final JsonArray durationList = Json.array();

        for (final PredefinedSpanUnit predefinedSpanUnit : PredefinedSpanUnit.values()) {
            final JsonObject jsonObject = Json.orderedObject();

            jsonObject.put(WorkflowAttributesV2.ID, predefinedSpanUnit.getId());
            jsonObject.put(WorkflowAttributesV2.NAME, predefinedSpanUnit.getName());
            jsonObject.put(WorkflowAttributesV2.HAS_DUE_TIME, predefinedSpanUnit.hasDueTime());
            jsonObject.put(WorkflowAttributesV2.HAS_SKIP_WEEKENDS, predefinedSpanUnit.hasSkipWeekends());

            durationList.put(jsonObject);
        }
        return durationList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowV2 convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);
        final WorkflowV2 workflow = workflowV2Converter.convert(converterConfig, jsonObject);

        workflow.setGraphUpdate(true);
        return workflow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowV2 convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return workflowV2Converter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest request, final WorkflowV2 workflow) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, null);

        return workflowV2Converter.convert(converterConfig, workflow);
    }

    /**
     * {@inheritDoc}
     */
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final JsonObject data, final EventData eventData) {
        try {
            workflowV2Manager.delete(orgId, data.getLong(WorkflowAttributesV2.ID), data.getBoolean(WorkflowAttributesV2.CANCEL_PENDING_EXECUTION));
        } catch (ErrorCode errorCode) {
            crmResponse.setErrorCode(errorCode);
            crmResponse.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
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
    protected final void fetchAndDispatchUser(final CrmRequest request, final WorkflowV2 workflow, final CrmResponse crmResponse) {
        final Long orgId = workflow.orgId();
        final JsonObject jsonObject = convert(request, workflow);

        loadWhatsappFieldValuesAndDispatch(orgId, workflow, jsonObject, crmResponse);
    }

    /**
     * Loads the whatsapp field values for the actions and dispatches the response.
     *
     * @param orgId        Represents the organization id.
     * @param workflowJson Represents the workflow details in json format.
     * @param crmResponse  Represents the response to be sent.
     */
    private void loadWhatsappFieldValuesAndDispatch(final Long orgId, final WorkflowV2 workflow, final JsonObject workflowJson, final CrmResponse crmResponse) {
        final Collection<Long> businessPhoneNumberIds = new HashSet<>(NumericalConstant.FIVE);
        final Collection<Long> templateIds = new HashSet<>(NumericalConstant.FIVE);
        final List<WorkflowNode> actionNodes = workflow.getNodesAsCollection().stream().filter(workflowNode -> Objects.equals(NodeType.ACTION, workflowNode.getNodeType())).toList();

        for (final WorkflowNode workflowNode : actionNodes) {
            final JsonObject actionJson = workflowJson.getJsonObject(WorkflowAttributesV2.NODES).getJsonObject(workflowNode.getClientKey());
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
                                for (final WorkflowNode workflowNode : actionNodes) {
                                    final JsonObject actionJson = workflowJson.getJsonObject(workflowNode.getClientKey());
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
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<WorkflowV2> workflows) {
        switch (action) {
            case CREATE, UPDATE -> {
                final Set<Long> templateIds = new HashSet<>();

                for (final WorkflowV2 workflow : workflows) {
                    final Long templateId = workflow.getTemplateId();

                    if (Objects.nonNull(templateId)) {
                        templateIds.add(templateId);
                    }
                }

                if (!templateIds.isEmpty() && !templateService.exists(templateIds)) {
                    throw ErrorCode.get(WorkflowErrorCodes.TEMPLATE_NOT_FOUND, "i18n.workflow.validation.invalid.template");
                }
            }
        }

        return true;
    }

    /**
     * Updates the workflow status.
     *
     * @param orgId the organization id
     * @param data  the data
     */
    private void updateWorkflowStatus(final long orgId, final JsonObject data) {
        final long id = data.getLong(WorkflowAttributesV2.ID); // TODO Stop pending execution when isActive=false

        if (workflowV2Manager.exists(orgId, id)) {
            workflowV2Manager.updateStatus(orgId, id, data.getBoolean(WorkflowAttributesV2.IS_ACTIVE), data.getBoolean(WorkflowAttributesV2.CANCEL_PENDING_EXECUTION));
        } else {
            throw ErrorCode.get(WorkflowV2ErrorCodes.WORKFLOW_NOT_FOUND, "i18n.workflow.not.found");
        }
    }
}

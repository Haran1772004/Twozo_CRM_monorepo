package com.tlc.crm.workflow.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.AuthorizationController;

import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.fields.service.settings.SourceSettingsService;

import com.tlc.crm.messenger.api.channel.manager.WABusinessAccountManager;
import com.tlc.crm.workflow.api.manager.WorkflowActionManager;
import com.tlc.crm.workflow.api.model.FieldGroup;
import com.tlc.crm.workflow.api.model.StepOption;
import com.tlc.crm.workflow.api.model.TriggerActionEvent;
import com.tlc.crm.workflow.api.model.TriggerActionField;
import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.cache.service.StepOptionService;
import com.tlc.crm.workflow.cache.service.TriggerActionEventService;
import com.tlc.crm.workflow.cache.service.TriggerActionFieldService;
import com.tlc.crm.workflow.cache.service.TriggerActionService;
import com.tlc.crm.workflow.cache.service.TriggerActionSourceService;
import com.tlc.crm.workflow.cache.service.WorkflowFieldService;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.resolver.internal.WorkflowActionResolver;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.i18n.I18nResolutionAdapter;

import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the workflow action related web actions.
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction(path = "/workflow/action/mgmt")
public class WorkflowActionManagement extends AuthorizationController {

    private static final String INDEX = "index";
    private static final String STEP_OPTIONS = "stepOptions";
    private static final String ALL_ACTIONS_I18N_KEY = "i18n.workflow.filter.all.actions";
    private static final int DEFAULT_FILTER_VALUE = -1;

    private final TriggerActionService triggerActionService;
    private final TriggerActionSourceService triggerActionSourceService;
    private final SourceSettingsService sourceSettingsService;
    private final EmailSyncManager emailSyncManager;
    private final WorkflowActionManager workflowActionManager;
    private final TriggerActionFieldService triggerActionFieldService;
    private final FieldService fieldService;
    private final WorkflowFieldService workflowFieldService;
    private final TriggerActionEventService triggerActionEventService;
    private final WorkflowActionResolver workflowActionResolver;
    private final StepOptionService stepOptionService;
    private final WABusinessAccountManager waBusinessAccountManager;

    public WorkflowActionManagement() {
        this.triggerActionService = TriggerActionService.getInstance();
        this.triggerActionSourceService = TriggerActionSourceService.getInstance();
        this.sourceSettingsService = SourceSettingsService.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
        this.workflowActionManager = WorkflowActionManager.getInstance();
        this.triggerActionFieldService = TriggerActionFieldService.getInstance();
        this.fieldService = FieldService.getInstance();
        this.workflowFieldService = WorkflowFieldService.getInstance();
        this.triggerActionEventService = TriggerActionEventService.getInstance();
        this.workflowActionResolver = WorkflowActionResolver.getInstance();
        this.stepOptionService = StepOptionService.getInstance();
        waBusinessAccountManager = WABusinessAccountManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request     Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void authorizedProcess(final CrmRequest request, final CrmResponse crmResponse) {
        final JsonObject requestJson = request.getRequestJson();
        final long orgId = request.orgId();
        final long userId = request.userId();
        final I18nResolutionAdapter i18NResolutionAdapter = request.getI18NCRMResolutionAdapter();
        final JsonObject data = requestJson.getJsonObject(WorkflowAttributes.DATA);
        final String type = requestJson.getString(WorkflowAttributes.TYPE);

        switch (type) {
            case "getActions" ->
                    crmResponse.put(WorkflowAttributes.LIST, getWorkflowActions(orgId, userId, data, crmResponse));
            case "getActionFields" -> resolveActionFieldsIntoResponse(orgId, data, request, crmResponse);
            case "getStepOptions" -> crmResponse.put(STEP_OPTIONS, getStepOptions(data, crmResponse));
            case "getStepOptionsV2" -> crmResponse.put(STEP_OPTIONS, getStepOptionsV2(data, crmResponse));
            case "getFilterActions" -> crmResponse.put(WorkflowAttributes.ACTIONS, getFilterActions(i18NResolutionAdapter));
        }
    }

    /**
     * <p>
     * Gets all the filter actions and return the json array
     * </p>
     *
     * @param i18NResolutionAdapter Represents the {@link I18nResolutionAdapter}
     * @return the json array which contains all filter actions
     */
    private JsonArray getFilterActions(final I18nResolutionAdapter i18NResolutionAdapter) {
        final JsonArray actionArray = Json.array();
        final JsonObject allActionJson = Json.object();

        allActionJson.put(WorkflowAttributes.VALUE, DEFAULT_FILTER_VALUE);
        allActionJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(ALL_ACTIONS_I18N_KEY));

        actionArray.put(allActionJson);

        for (final TriggerActionEvent triggerActionEvent : triggerActionEventService.getAll()) {
            final JsonObject actionEventJson = Json.object();

            actionEventJson.put(WorkflowAttributes.VALUE, triggerActionEvent.id());
            actionEventJson.put(WorkflowAttributes.NAME, i18NResolutionAdapter.get(triggerActionEvent.getDisplayName()));

            actionArray.put(actionEventJson);
        }

        return actionArray;
    }

    /**
     * <p>
     * Resolves the {@link TriggerActionField} into the {@link CrmResponse}.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param data        The {@link JsonObject} containing workflow-related data
     * @param crmRequest  Refers the {@link CrmRequest}
     * @param crmResponse The {@link CrmResponse} object to populate with the execution result
     */
    private void resolveActionFieldsIntoResponse(final long orgId, final JsonObject data, final CrmRequest crmRequest,
                                                 final CrmResponse crmResponse) {
        final JsonObject actionJson = data.getJsonObject(WorkflowAttributes.ACTION);
        final short triggerSourceId = data.getShort(WorkflowAttributes.TRIGGER_SOURCE);
        final long triggerActionSourceId = actionJson.getLong(WorkflowAttributes.SOURCE);
        final long triggerActionEventId = actionJson.getLong(WorkflowAttributes.EVENT);
        final I18nResolutionAdapter i18NResolutionAdapter = crmRequest.getI18NCRMResolutionAdapter();
        final Long triggerActionId = workflowActionManager.getTriggerActionId(triggerSourceId, triggerActionSourceId, triggerActionEventId);

        if (Objects.nonNull(triggerActionId)) {
            final Collection<TriggerActionField> triggerActionFields = triggerActionFieldService.get(triggerActionId);
            final String triggerActionSourceName = triggerActionSourceService.get(triggerActionSourceId).getAliasName();

            if (Objects.equals(WorkflowAttributes.WHATSAPP, triggerActionSourceName) || (Objects.equals(WorkflowAttributes.EMAIL, triggerActionSourceName) &&
                    Objects.equals(WorkflowAttributes.EMAIL_SEND, triggerActionEventService.get(triggerActionEventId).getAliasName()))) {
                final JsonObject triggerActionFieldsJson = Json.object();

                buildTriggerActionFieldsJson(orgId, triggerActionFields, i18NResolutionAdapter, triggerActionFieldsJson);

                crmResponse.put(WorkflowAttributes.FIELDS, triggerActionFieldsJson);
            } else {
                final JsonArray triggerActionFieldsArray = Json.array();
                final boolean isCommonActionSource = triggerActionSourceService.get(triggerActionSourceId).isCommonActionSource();
                final boolean isCommonActionEvent = triggerActionEventService.get(triggerActionEventId).isCommonActionEvent();
                buildTriggerActionFieldsArray(orgId, isCommonActionSource, triggerActionFields, i18NResolutionAdapter, triggerActionFieldsArray);

                if (isCommonActionSource && isCommonActionEvent) {
                    workflowActionResolver.loadPersistentFields(orgId, triggerActionSourceId, i18NResolutionAdapter, triggerActionFieldsArray, null);
                }
                crmResponse.put(WorkflowAttributes.FIELDS, triggerActionFieldsArray);
            }
        } else {
            throw ErrorCode.get(WorkflowErrorCodes.TRIGGER_ACTION_NOT_FOUND, "i18n.workflow.action.not.found");
        }
    }

    /**
     * <p>
     * Builds a {@link JsonObject} containing {@link TriggerActionField} for a given organization
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param triggerActionFields     Represents the collection of {@link TriggerActionField}
     * @param i18NResolutionAdapter Represents the resolver of the I18N strings.
     * @param triggerActionFieldsJson The {@link JsonObject} to populate with the built trigger action fields
     */
    private void buildTriggerActionFieldsJson(final Long orgId, final Collection<TriggerActionField> triggerActionFields,
                                              final I18nResolutionAdapter i18NResolutionAdapter, final JsonObject triggerActionFieldsJson) {
        for (final TriggerActionField triggerActionField : triggerActionFields) {
            final JsonObject triggerActionFieldJson = Json.object();

            workflowActionResolver.loadActionFieldToJson(orgId, triggerActionFieldJson, i18NResolutionAdapter, null, triggerActionField);

            triggerActionFieldsJson.put(workflowFieldService.get(triggerActionField.getFieldId()).getAliasName(), triggerActionFieldJson);
        }
    }

    /**
     * <p>
     * Builds a {@link JsonArray} containing {@link TriggerActionField} for a given organization
     * </p>
     *
     * @param orgId                   Represents the organization id
     * @param isCommonActionSource    A boolean flag indicating whether the action source are common or not
     * @param triggerActionFields     Represents the collection of {@link TriggerActionField}
     * @param i18NResolutionAdapter Represents the resolver to resolve I18N strings
     * @param actionFieldsArray       The {@link JsonArray} to populate with the built trigger action fields
     */
    private void buildTriggerActionFieldsArray(final Long orgId, final boolean isCommonActionSource, final Collection<TriggerActionField> triggerActionFields,
                                               final I18nResolutionAdapter i18NResolutionAdapter, final JsonArray actionFieldsArray) {
        for (final Map.Entry<Integer, List<TriggerActionField>> triggerActionFieldEntry : triggerActionFieldService.groupActionFieldsByConfig(triggerActionFields).entrySet()) {
            final JsonArray fieldsArray = Json.array();

            for (final TriggerActionField triggerActionField : triggerActionFieldEntry.getValue()) {
                final JsonObject triggerActionFieldJson = Json.object();
                final FieldInfo fieldInfo = isCommonActionSource && FieldGroup.SOURCE_RELATED_FIELD == triggerActionField.getFieldGroup()
                        ? fieldService.getField(orgId, triggerActionField.getFieldId()) : null;

                workflowActionResolver.loadActionFieldToJson(orgId, triggerActionFieldJson, i18NResolutionAdapter, fieldInfo, triggerActionField);

                fieldsArray.put(triggerActionFieldJson);
            }
            actionFieldsArray.put(fieldsArray);
        }
    }

    /**
     * <p>
     * Retrieves the workflow actions for a given organization id
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param jsonObject  Which contains the requested data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private JsonArray getWorkflowActions(final long orgId, final long userId, final JsonObject jsonObject,
                                         final CrmResponse crmResponse) {
        final short triggerSourceId = jsonObject.getShort(WorkflowAttributes.TRIGGER_SOURCE);
        final TriggerSource triggerSource = TriggerSource.get(triggerSourceId);
        final TriggerSource.TriggerEvent triggerEvent = TriggerSource.TriggerEvent.get(jsonObject.getShort(WorkflowAttributes.TRIGGER_EVENT));

        if (!triggerActionService.exists(triggerSourceId)) {
            throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_SOURCE, crmResponse.resolveI18nString("i18n.workflow.invalid.trigger.source"));
        }
        final JsonArray triggerActionsArray = Json.array();
        final Collection<TriggerActionSource> triggerActionSources;

        if (jsonObject.containsKey(WorkflowAttributes.ACTION_SOURCES)) {
            final JsonArray triggerActionSourceIdArray = jsonObject.getJsonArray(WorkflowAttributes.ACTION_SOURCES);
            final Collection<Long> triggerActionSourceIds = new HashSet<>();

            for (int index = 0; index < triggerActionSourceIdArray.size(); index++) {
                triggerActionSourceIds.add(triggerActionSourceIdArray.getLong(index));
            }

            if (!triggerActionSourceService.exists(triggerActionSourceIds)) {
                throw ErrorCode.get(WorkflowErrorCodes.ACTION_SOURCE_NOT_FOUND, crmResponse.resolveI18nString("i18n.workflow.invalid.action.source"));
            }

            if (Objects.equals(TriggerSource.ACTIVITY, triggerSource) ||
                    triggerActionSourceService.isAliasNameMatch(triggerActionSourceIds, WorkflowAttributes.ACTIVITY)) {
                triggerActionSources = triggerActionService.getActionSourceByTriggerSource(TriggerSource.ACTIVITY.getId());
            } else if (triggerActionSourceService.isAliasNameMatch(triggerActionSourceIds, WorkflowAttributes.DEAL)) {
                triggerActionSources = triggerActionService.getActionSourceByTriggerSource(TriggerSource.DEAL.getId());
            } else {
                triggerActionSources = triggerActionService.getActionSourceByTriggerSource(triggerSourceId);
            }
        } else {
            triggerActionSources = triggerActionService.getActionSourceByTriggerSource(triggerSourceId);
        }

        if (Objects.nonNull(triggerActionSources)) {
            loadTriggerActionsArray(orgId, userId, triggerActionSources, triggerActionsArray, crmResponse, triggerSource, triggerEvent);
        }

        return triggerActionsArray;
    }

    /**
     * <p>
     * Loads trigger actions into a {@link JsonArray}
     * </p>
     *
     * @param orgId                Represents the organization id
     * @param userId               Represents the user id
     * @param triggerActionSources Represents the collection of {@link TriggerActionSource}
     * @param actionsArray         The {@link JsonArray} to populate with loaded actions
     * @param crmResponse          The {@link CrmResponse} to send back to the client
     */
    private void loadTriggerActionsArray(final Long orgId, final Long userId, final Collection<TriggerActionSource> triggerActionSources,
                                         final JsonArray actionsArray, final CrmResponse crmResponse, final TriggerSource triggerSource,
                                         final TriggerSource.TriggerEvent triggerEvent) {
        final boolean isProductEnabled = sourceSettingsService.isSourceEnabled(orgId, Source.PRODUCT.getName());
        final boolean hasSyncedEmail = emailSyncManager.isEmailSyncEnabled(orgId, userId);

        for (final TriggerActionSource triggerActionSource : triggerActionSources) {
            final JsonObject triggerActionSourceJson = Json.object();
            final JsonArray eventsArray = Json.array();

            triggerActionSourceJson.put(WorkflowAttributes.ID, triggerActionSource.id());
            triggerActionSourceJson.put(WorkflowAttributes.NAME, crmResponse.resolveI18nPlaceholder(triggerActionSource.getDisplayName()));

            for (final TriggerActionEvent triggerActionEvent : triggerActionSource.getTriggerActionEvents()) {
                final JsonObject eventJson = Json.object();
                final String triggerActionSourceAliasName = triggerActionSource.getAliasName();

                eventJson.put(WorkflowAttributes.ID, triggerActionEvent.id());
                eventJson.put(WorkflowAttributes.NAME, crmResponse.resolveI18nPlaceholder(triggerActionEvent.getDisplayName()));

                final boolean isEmailActionEnabled = Objects.equals(WorkflowAttributes.EMAIL, triggerActionSourceAliasName) && hasSyncedEmail;
                final boolean isAddProductToDealEnabled = Objects.equals(WorkflowAttributes.DEAL, triggerActionSourceAliasName) && isProductEnabled
                        && Objects.equals(WorkflowAttributes.ADD_PRODUCT_TO_DEAL, triggerActionEvent.getAliasName());
                final boolean isWhatsappActionEnabled = Objects.equals(WorkflowAttributes.WHATSAPP, triggerActionSourceAliasName)
                        && ((Objects.equals(TriggerSource.CONTACT, triggerSource) && !Objects.equals(TriggerSource.TriggerEvent.DELETED, triggerEvent)) || (Objects.equals(TriggerSource.DEAL, triggerSource) && !Objects.equals(TriggerSource.TriggerEvent.DELETED, triggerEvent)))
                        && waBusinessAccountManager.arePhoneNumbersActive(orgId);

                if (isEmailActionEnabled || isAddProductToDealEnabled || isWhatsappActionEnabled) {
                    eventJson.put(WorkflowAttributes.IS_ENABLED, true);
                } else {
                    eventJson.put(WorkflowAttributes.IS_ENABLED, triggerActionEvent.isEnabled());
                }
                eventsArray.put(eventJson);
            }
            triggerActionSourceJson.put(WorkflowAttributes.EVENT, eventsArray);

            actionsArray.put(triggerActionSourceJson);
        }
    }

    /**
     * <p>
     * Gets the {@link StepOption} based on the information provided in the input {@link JsonObject}
     * </p>
     *
     * @param jsonObject  The {@link JsonObject} containing workflow information
     * @param crmResponse Represents the {@link CrmResponse}
     * @return A {@link JsonArray} containing the {@link StepOption} extracted from the {@link JsonObject}
     */
    private JsonArray getStepOptions(final JsonObject jsonObject, final CrmResponse crmResponse) {
        final short triggerSourceId = jsonObject.getShort(WorkflowAttributes.TRIGGER_SOURCE);
        final long fieldId = jsonObject.getLong(WorkflowAttributes.FIELD_ID);
        final JsonArray actionsArray = jsonObject.getJsonArray(WorkflowAttributes.ACTIONS);
        final JsonArray stepOptionArray = Json.array();
        final int actionsSize = actionsArray.size();

        if (0 < actionsSize) {
            final JsonObject stepOptionActionJson = actionsArray.getJsonObject(actionsSize - 1);
            final long stepOptionActionSourceId = stepOptionActionJson.getLong(WorkflowAttributes.SOURCE);
            final long stepOptionActionEventId = stepOptionActionJson.getLong(WorkflowAttributes.EVENT);

            if (!triggerActionService.exists(triggerSourceId)) {
                throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_SOURCE, "i18n.workflow.invalid.trigger.source");
            }

            if (!triggerActionSourceService.exists(stepOptionActionSourceId)) {
                throw ErrorCode.get(WorkflowErrorCodes.ACTION_SOURCE_NOT_FOUND, "i18n.workflow.invalid.action.source");
            }

            if (!triggerActionService.isEventIdExists(triggerSourceId, stepOptionActionSourceId, stepOptionActionEventId)) {
                throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
            }
            resolveStepOptionArray(actionsSize, triggerSourceId, stepOptionArray, true, crmResponse,
                    triggerActionFieldService.get(workflowActionManager.getTriggerActionId(triggerSourceId, stepOptionActionSourceId, stepOptionActionEventId), fieldId));
            actionsArray.remove(actionsSize - 1);
            final int actionsArraySize = actionsArray.size();

            for (int actionIndex = 0; actionIndex < actionsArraySize; actionIndex++) {
                final long triggerActionSourceId = actionsArray.getJsonObject(actionIndex).getLong(WorkflowAttributes.SOURCE);
                final TriggerActionSource triggerActionSource = triggerActionSourceService.get(triggerActionSourceId);

                if (Objects.nonNull(triggerActionSource)) {
                    final TriggerSource triggerSource = TriggerSource.getByName(triggerActionSource.getAliasName());

                    if (Objects.nonNull(triggerSource)) {
                        final short sourceId = triggerSource.getId();

                        if (!triggerActionService.isEventIdExists(sourceId, stepOptionActionSourceId, stepOptionActionEventId)) {
                            throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
                        }
                        resolveStepOptionArray(actionIndex, sourceId, stepOptionArray, false, crmResponse,
                                triggerActionFieldService.get(workflowActionManager.getTriggerActionId(sourceId, stepOptionActionSourceId, stepOptionActionEventId), fieldId));
                    }
                } else {
                    throw ErrorCode.get(WorkflowErrorCodes.ACTION_SOURCE_NOT_FOUND, "i18n.workflow.invalid.action.source");
                }
            }
        }
        return stepOptionArray;
    }

    /**
     * Gets the version-2 {@link StepOption} based on the information provided in the input {@link JsonObject}
     *
     * @param jsonObject  The {@link JsonObject} containing workflow information
     * @param crmResponse Represents the {@link CrmResponse}
     * @return A {@link JsonArray} containing the {@link StepOption} extracted from the {@link JsonObject}
     */
    private JsonArray getStepOptionsV2(final JsonObject jsonObject, final CrmResponse crmResponse) {
        final JsonObject triggerJson = jsonObject.getJsonObject(WorkflowAttributes.TRIGGER);
        final short triggerSourceId = triggerJson.getShort(WorkflowAttributes.SOURCE);
        final long fieldId = jsonObject.getLong(WorkflowAttributes.FIELD_ID);
        final JsonArray actionsArray = jsonObject.getJsonArray(WorkflowAttributes.ACTIONS);
        final JsonArray stepOptionArray = Json.array();
        final int actionsSize = actionsArray.size();

        if (0 < actionsSize) {
            final JsonObject stepOptionActionJson = actionsArray.getJsonObject(actionsSize - 1);
            final long stepOptionActionSourceId = stepOptionActionJson.getLong(WorkflowAttributes.SOURCE);
            final long stepOptionActionEventId = stepOptionActionJson.getLong(WorkflowAttributes.EVENT);

            if (!triggerActionService.exists(triggerSourceId)) {
                throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_SOURCE, "i18n.workflow.invalid.trigger.source");
            }

            if (!triggerActionSourceService.exists(stepOptionActionSourceId)) {
                throw ErrorCode.get(WorkflowErrorCodes.ACTION_SOURCE_NOT_FOUND, "i18n.workflow.invalid.action.source");
            }

            if (!triggerActionService.isEventIdExists(triggerSourceId, stepOptionActionSourceId, stepOptionActionEventId)) {
                throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
            }
            loadStepOptionsV2(triggerJson, triggerSourceId, stepOptionArray, true, crmResponse,
                    triggerActionFieldService.get(workflowActionManager.getTriggerActionId(triggerSourceId, stepOptionActionSourceId, stepOptionActionEventId), fieldId));
            actionsArray.remove(actionsSize - 1);
            final int actionsArraySize = actionsArray.size();

            for (int actionIndex = 0; actionIndex < actionsArraySize; actionIndex++) {
                final JsonObject actionJson = actionsArray.getJsonObject(actionIndex);
                final long triggerActionSourceId = actionJson.getLong(WorkflowAttributes.SOURCE);
                final TriggerActionSource triggerActionSource = triggerActionSourceService.get(triggerActionSourceId);

                if (Objects.nonNull(triggerActionSource)) {
                    final TriggerSource triggerSource = TriggerSource.getByName(triggerActionSource.getAliasName());

                    if (Objects.nonNull(triggerSource)) {
                        final short sourceId = triggerSource.getId();

                        if (!triggerActionService.isEventIdExists(sourceId, stepOptionActionSourceId, stepOptionActionEventId)) {
                            throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
                        }
                        loadStepOptionsV2(actionJson, sourceId, stepOptionArray, false, crmResponse,
                                triggerActionFieldService.get(workflowActionManager.getTriggerActionId(sourceId, stepOptionActionSourceId, stepOptionActionEventId), fieldId));
                    }
                } else {
                    throw ErrorCode.get(WorkflowErrorCodes.ACTION_SOURCE_NOT_FOUND, "i18n.workflow.invalid.action.source");
                }
            }
        }
        return stepOptionArray;
    }

    /**
     * <p>
     * Resolves a {@link JsonArray} of {@link StepOption} based on the provided parameters.
     * </p>
     *
     * @param jsonObject         Represents the trigger/action json.
     * @param triggerActionField It holds the {@link TriggerActionField} details
     * @param triggerSourceId    Represents the {@link TriggerSource} id
     * @param stepOptionArray    Represents the array of {@link StepOption}
     * @param dependsOnTrigger   A boolean indicating whether the {@link StepOption} depend on the trigger.
     * @param crmResponse        The {@link CrmResponse} to send back to the client
     */
    private void loadStepOptionsV2(final JsonObject jsonObject, final short triggerSourceId, final JsonArray stepOptionArray,
                                   final boolean dependsOnTrigger, final CrmResponse crmResponse, final TriggerActionField triggerActionField) {
        final Collection<Long> stepOptionIds = Objects.isNull(triggerActionField) ? null : triggerActionField.getStepOptionIds();

        if (Objects.nonNull(stepOptionIds) && !stepOptionIds.isEmpty()) {
            final JsonArray stepOptionFieldsArray = Json.array();
            final JsonObject stepOptionJson = Json.object();
            final JsonObject dependsOnJson = Json.object();

            for (final StepOption stepOption : stepOptionService.getStepOptions((Set<Long>) stepOptionIds)) {
                final JsonObject stepOptionFieldJson = Json.object();

                stepOptionFieldJson.put(WorkflowAttributes.ID, stepOption.id());
                stepOptionFieldJson.put(WorkflowAttributes.NAME, crmResponse.resolveI18nPlaceholder(stepOption.name()));

                stepOptionFieldsArray.put(stepOptionFieldJson);
            }
            stepOptionJson.put(WorkflowAttributes.SOURCE, crmResponse.resolveI18nPlaceholder(TriggerSource.get(triggerSourceId).getI18nKey()));
            stepOptionJson.put(WorkflowAttributes.FIELDS, stepOptionFieldsArray);

            if (dependsOnTrigger) {
                dependsOnJson.put(WorkflowAttributes.TYPE, WorkflowAttributes.TRIGGER);
            } else {
                dependsOnJson.put(WorkflowAttributes.TYPE, WorkflowAttributes.ACTION);
            }
            dependsOnJson.put(WorkflowAttributesV2.NODE, jsonObject.getShort(WorkflowAttributesV2.NODE));
            stepOptionJson.put(WorkflowAttributes.DEPENDS_ON, dependsOnJson);

            stepOptionArray.put(stepOptionJson);
        }
    }

    /**
     * <p>
     * Resolves a {@link JsonArray} of {@link StepOption} based on the provided parameters.
     * </p>
     *
     * @param actionSize         Represents the index of the action.
     * @param triggerActionField It holds the {@link TriggerActionField} details
     * @param triggerSourceId    Represents the {@link TriggerSource} id
     * @param stepOptionArray    Represents the array of {@link StepOption}
     * @param dependsOnTrigger   A boolean indicating whether the {@link StepOption} depend on the trigger.
     * @param crmResponse        The {@link CrmResponse} to send back to the client
     */
    private void resolveStepOptionArray(final int actionSize, final short triggerSourceId, final JsonArray stepOptionArray,
                                        final boolean dependsOnTrigger, final CrmResponse crmResponse, final TriggerActionField triggerActionField) {
        final Collection<Long> stepOptionIds = Objects.isNull(triggerActionField) ? null : triggerActionField.getStepOptionIds();

        if (Objects.nonNull(stepOptionIds) && !stepOptionIds.isEmpty()) {
            final JsonArray stepOptionFieldsArray = Json.array();
            final JsonObject stepOptionJson = Json.object();
            final JsonObject dependsOnJson = Json.object();

            for (final StepOption stepOption : stepOptionService.getStepOptions((Set<Long>) stepOptionIds)) {
                final JsonObject stepOptionFieldJson = Json.object();

                stepOptionFieldJson.put(WorkflowAttributes.ID, stepOption.id());
                stepOptionFieldJson.put(WorkflowAttributes.NAME, crmResponse.resolveI18nPlaceholder(stepOption.name()));

                stepOptionFieldsArray.put(stepOptionFieldJson);
            }
            stepOptionJson.put(WorkflowAttributes.SOURCE, crmResponse.resolveI18nPlaceholder(TriggerSource.get(triggerSourceId).getI18nKey()));
            stepOptionJson.put(WorkflowAttributes.FIELDS, stepOptionFieldsArray);

            if (dependsOnTrigger) {
                dependsOnJson.put(WorkflowAttributes.TYPE, WorkflowAttributes.TRIGGER);
            } else {
                dependsOnJson.put(WorkflowAttributes.TYPE, WorkflowAttributes.ACTION);
                dependsOnJson.put(INDEX, actionSize);
            }
            stepOptionJson.put(WorkflowAttributes.DEPENDS_ON, dependsOnJson);

            stepOptionArray.put(stepOptionJson);
        }
    }
}

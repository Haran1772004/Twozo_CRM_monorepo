package com.tlc.crm.workflow.api.model.converter.history;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionHistory;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowActionExecutionStatus;
import com.tlc.crm.workflow.field.WorkflowAttributes;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Collection;
import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides different ways to convert the data from/to {@link WorkflowExecutionHistory}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowExecutionHistoryConverter {

    private static final String SUCCESS_I18N_KEY = "i18n.workflow.execution.criteria.state.success";
    private static final String FAILURE_I18N_KEY = "i18n.workflow.execution.criteria.state.failed";
    private static final String TRIGGER_STATE_I18N_KEY = "i18n.workflow.execution.started";

    private final DateTimeUtil dateTimeUtil;

    private WorkflowExecutionHistoryConverter() {
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final WorkflowExecutionHistoryConverter INSTANCE = new WorkflowExecutionHistoryConverter();
    }

    /**
     * <p>
     * Gets the {@link WorkflowExecutionHistoryConverter} Instance
     * </p>
     *
     * @return {@link WorkflowExecutionHistoryConverter}
     */
    public static WorkflowExecutionHistoryConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the given {@link WorkflowExecutionHistory} into {@link JsonObject}
     * </p>
     *
     * @param workflowExecutionHistory Represents the {@link WorkflowExecutionHistory}
     * @return {@link JsonObject} contains the {@link WorkflowExecutionHistory} details
     */
    public JsonObject convert(final WorkflowExecutionHistory workflowExecutionHistory, final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final JsonObject workflowStateJson = workflowExecutionHistory.getWorkflowState();
        final Collection<WorkflowActionExecutionHistory> workflowActionExecutionHistories = workflowExecutionHistory.getActionExecutionHistories();

        jsonObject.put(WorkflowAttributes.ID, workflowExecutionHistory.id())
                .put(WorkflowAttributes.WORKFLOW_ID, workflowExecutionHistory.getWorkflowId())
                .put(WorkflowAttributes.NAME, i18nResolver.get(workflowStateJson.getString(WorkflowAttributes.NAME)))
                .put(WorkflowAttributes.DESCRIPTION, workflowStateJson.containsKey(WorkflowAttributes.DESCRIPTION)
                        ? i18nResolver.get(workflowStateJson.getString(WorkflowAttributes.DESCRIPTION)) : null)
                .put(WorkflowAttributes.TRIGGER, getTrigger(workflowExecutionHistory, workflowStateJson.getJsonObject(WorkflowAttributes.TRIGGER), i18nResolver));

        if (workflowStateJson.containsKey(WorkflowAttributes.CONDITIONS)) {
            final JsonObject conditionHistoryJson = getCondition(workflowExecutionHistory, i18nResolver);

            conditionHistoryJson.put(WorkflowAttributes.CONDITIONS, workflowStateJson.getJsonArray(WorkflowAttributes.CONDITIONS));
            jsonObject.put(WorkflowAttributes.CONDITION, conditionHistoryJson);
        }

        if (Objects.nonNull(workflowActionExecutionHistories)) {
            jsonObject.put(WorkflowAttributes.ACTIONS, getActions(workflowActionExecutionHistories, workflowStateJson.getJsonArray(WorkflowAttributes.ACTIONS), i18nResolver));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the {@link JsonArray} which contains the details of the action
     * </p>
     *
     * @param workflowActionExecutionHistories Represents the {@link Collection} of {@link WorkflowActionExecutionHistory}
     * @param actionArray                      Refers the workflow action {@link JsonArray}.
     * @param i18nResolver                     Represents the {@link I18nResolver}
     * @return {@link JsonArray} contains the values of actions
     */
    private JsonArray getActions(final Collection<WorkflowActionExecutionHistory> workflowActionExecutionHistories,
                                 final JsonArray actionArray, final I18nResolver i18nResolver) {
        final JsonArray jsonArray = Json.array();
        final Map<Long, WorkflowActionExecutionHistory> groupedHistories = workflowActionExecutionHistories.stream()
                .collect(Collectors.toMap(WorkflowActionExecutionHistory::getActionId, Function.identity()));

        for (int index = 0; index < actionArray.size(); index++) {
            final JsonObject jsonObject = Json.object();
            final JsonObject actionJson = actionArray.getJsonObject(index);
            final JsonObject actionEventJson = actionJson.getJsonObject(WorkflowAttributes.EVENT);
            final String actionEventName = i18nResolver.get(actionEventJson.getString(WorkflowAttributes.NAME));
            final WorkflowActionExecutionHistory workflowActionExecutionHistory = groupedHistories.get(actionJson.getLong(WorkflowAttributes.ID));

            if (Objects.nonNull(workflowActionExecutionHistory)) {
                final WorkflowActionExecutionFailureCause executionFailureCause = workflowActionExecutionHistory.getFailureCause();
                final WorkflowActionExecutionStatus workflowActionExecutionStatus = workflowActionExecutionHistory.getActionExecutionStatus();

                jsonObject.put(WorkflowAttributes.EVENT, Json.object().put(WorkflowAttributes.NAME, actionEventName)
                                .put(WorkflowAttributes.ID, actionEventJson.getLong(WorkflowAttributes.ID)))
                        .put(WorkflowAttributes.SOURCE, actionJson.getJsonObject(WorkflowAttributes.SOURCE))
                        .put(WorkflowAttributes.STATE, i18nResolver.get(workflowActionExecutionStatus.getName()))
                        .put(WorkflowAttributes.STATUS, i18nResolver.get(WorkflowActionExecutionStatus.SUCCESS == workflowActionExecutionStatus ? SUCCESS_I18N_KEY : FAILURE_I18N_KEY))
                        .put(WorkflowAttributes.EXECUTED_TIME, workflowActionExecutionHistory.getExecutedTime());

                if (Objects.nonNull(executionFailureCause)) {
                    jsonObject.put(WorkflowAttributes.REASON, i18nResolver.get(executionFailureCause.getFailureCause()));
                }
            } else {
                jsonObject.put(WorkflowAttributes.EVENT, Json.object().put(WorkflowAttributes.NAME, actionEventName)
                        .put(WorkflowAttributes.ID, actionEventJson.getLong(WorkflowAttributes.ID)));
                jsonObject.put(WorkflowAttributes.SOURCE, actionJson.getJsonObject(WorkflowAttributes.SOURCE));
            }

            jsonArray.put(jsonObject);
        }
        return jsonArray;
    }

    /**
     * <p>
     * Gets the {@link JsonObject} which contains the details of the trigger
     * </p>
     *
     * @param workflowExecutionHistory Represents the {@link WorkflowExecutionHistory}
     * @param i18nResolver             Represents the {@link I18nResolver}
     * @return {@link JsonObject} contains the values of trigger
     */
    private JsonObject getTrigger(final WorkflowExecutionHistory workflowExecutionHistory, final JsonObject jsonObject, final I18nResolver i18nResolver) {
        return jsonObject.put(WorkflowAttributes.STATE, i18nResolver.get(TRIGGER_STATE_I18N_KEY))
                .put(WorkflowAttributes.EXECUTED_TIME, dateTimeUtil.getTimeStamp(workflowExecutionHistory.getExecutedTime()));
    }

    /**
     * <p>
     * Gets the {@link JsonObject} which contains the details of the conditions
     * </p>
     *
     * @param workflowExecutionHistory Represents the {@link WorkflowExecutionHistory}
     * @return {@link JsonObject}  contains the values of conditions
     */
    private JsonObject getCondition(final WorkflowExecutionHistory workflowExecutionHistory, final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();
        final WorkflowCriteriaExecutionStatus criteriaExecutionStatus = workflowExecutionHistory.getCriteriaExecutionStatus();

        jsonObject.put(WorkflowAttributes.EXECUTED_TIME, dateTimeUtil.getTimeStamp(workflowExecutionHistory.getConditionExecutedTime()))
                .put(WorkflowAttributes.STATUS, i18nResolver.get(WorkflowCriteriaExecutionStatus.PASSED == criteriaExecutionStatus ? SUCCESS_I18N_KEY : FAILURE_I18N_KEY))
                .put(WorkflowAttributes.STATE, i18nResolver.get(criteriaExecutionStatus.getName()));

        return jsonObject;
    }
}

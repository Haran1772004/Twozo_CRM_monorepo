package com.tlc.crm.workflow.v2.api.model.converter.history;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowActionExecutionStatus;
import com.tlc.crm.workflow.api.model.history.WorkflowExecutionStatus.WorkflowCriteriaExecutionStatus;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.v2.api.model.ExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.NodeExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2ExecutionNode;
import com.tlc.crm.workflow.v2.api.model.converter.WorkflowV2Converter;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Converts V2 executions into the V1 history JSON shape.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2ExecutionHistoryConverter {

    private static final String TRIGGER_STATE_I18N_KEY = "i18n.workflow.execution.started";

    private final DateTimeUtil dateTimeUtil;
    private final WorkflowV2Converter workflowV2Converter;

    private WorkflowV2ExecutionHistoryConverter() {
        this.dateTimeUtil = DateTimeUtil.getInstance();
        this.workflowV2Converter = WorkflowV2Converter.getInstance();
    }

    private static final class Instance {
        private static final WorkflowV2ExecutionHistoryConverter INSTANCE = new WorkflowV2ExecutionHistoryConverter();
    }

    /**
     * Returns the instance of the WorkflowV2ExecutionHistoryConverter.
     *
     * @return The instance of the WorkflowV2ExecutionHistoryConverter.
     */
    public static WorkflowV2ExecutionHistoryConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the execution to a JSON object.
     *
     * @param execution    The execution.
     * @param workflow     The workflow.
     * @param locale       The locale.
     * @param i18nResolver The i18n resolver.
     * @return The JSON object.
     */
    public JsonObject convert(final WorkflowV2Execution execution, final WorkflowV2 workflow,
                              final Locale locale, final I18nResolver i18nResolver) {
        final ConverterConfig converterConfig = new ConverterConfig(workflow.orgId(), execution.getUserId(), locale, false, null);
        final JsonObject jsonObject = workflowV2Converter.convert(converterConfig, workflow);
        final JsonObject nodeJson = jsonObject.getJsonObject(WorkflowAttributesV2.NODES);
        final Map<Long, String> nodeIds = workflow.getIdToClientKey();

        jsonObject.put(WorkflowAttributes.ID, execution.id())
                .put(WorkflowAttributes.WORKFLOW_ID, execution.getWorkflowId());

        for (final WorkflowV2ExecutionNode executionNode : execution.getExecutionNodes()) {
            final Long nodeId = executionNode.getNodeId();
            final JsonObject json = nodeJson.getJsonObject(nodeIds.get(nodeId));
            final NodeType nodeType = NodeType.get(json.getShort(WorkflowAttributesV2.NODE_TYPE));
            final NodeExecutionStatus nodeExecutionStatus = executionNode.getStatus();
            final DateTime startedTime = executionNode.getStartedTime();

            json.put(WorkflowAttributes.EXECUTED_TIME, dateTimeUtil.getTimeStamp(startedTime));

            switch (nodeType) {
                case TRIGGER -> json.put(WorkflowAttributes.STATE, i18nResolver.get(TRIGGER_STATE_I18N_KEY));
                case INSTANT_CONDITION, IF_ELSE_CONDITION -> {
                    final WorkflowCriteriaExecutionStatus status = NodeExecutionStatus.FAILED == nodeExecutionStatus
                            ? WorkflowCriteriaExecutionStatus.FAILED : WorkflowCriteriaExecutionStatus.PASSED;

                    json.put(WorkflowAttributes.STATE, i18nResolver.get(status.getName()));
                    json.put(WorkflowAttributes.STATUS, nodeExecutionStatus.name());
                }
                case ACTION -> {
                    final ExecutionStatus executionStatus = execution.getStatus();
                    final Short failureReason = executionNode.getFailureReason();

                    if (ExecutionStatus.STOPPED == executionStatus) {
                        json.put(WorkflowAttributes.STATE, i18nResolver.get(executionStatus.getDisplayName()));
                        json.put(WorkflowAttributes.STATUS, executionStatus.name());
                    } else {
                        final WorkflowActionExecutionStatus actionStatus = NodeExecutionStatus.COMPLETED == nodeExecutionStatus
                                ? WorkflowActionExecutionStatus.SUCCESS : WorkflowActionExecutionStatus.FAILURE;

                        json.put(WorkflowAttributes.STATE, i18nResolver.get(actionStatus.getName()));
                        json.put(WorkflowAttributes.STATUS, nodeExecutionStatus.name());
                    }

                    if (Objects.nonNull(failureReason)) {
                        json.put(WorkflowAttributes.REASON, i18nResolver.get(WorkflowActionExecutionFailureCause.get(failureReason).getFailureCause()));
                    }
                }
                case DELAY, WAIT_FOR_CONDITION -> {
                    final ExecutionStatus executionStatus = execution.getStatus();

                    if (ExecutionStatus.STOPPED == executionStatus) {
                        json.put(WorkflowAttributes.STATE, i18nResolver.get(executionStatus.getDisplayName()));
                        json.put(WorkflowAttributes.STATUS, executionStatus.name());
                    } else {
                        json.put(WorkflowAttributes.STATE, i18nResolver.get(nodeExecutionStatus.getName()));
                        json.put(WorkflowAttributes.STATUS, nodeExecutionStatus.name());
                    }
                }
            }
        }

        return jsonObject;
    }

}

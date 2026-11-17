package com.tlc.crm.workflow.v2.internal.async.req;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTracker;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;

import com.tlc.crm.entity.fields.FieldKeys;

import com.tlc.crm.workflow.api.model.history.WorkflowActionExecutionFailureCause;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.execution.AsyncActionCompletionGateway;
import com.tlc.crm.workflow.v2.internal.execution.WorkflowV2ExecutionEngine;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The {@code WorkflowV2EmailActionListener} is responsible for listening to the email action requests.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 */
public final class WorkflowV2EmailActionListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2EmailActionListener.class);
    private final Executor executor;
    private final WorkflowV2ExecutionEngine executionEngine;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;
    private final WorkflowV2ExecutionManager executionManager;
    private final AsyncActionCompletionGateway actionCompletionGateway;

    public WorkflowV2EmailActionListener(final Executor executor) {
        this.executor = executor;
        this.executionEngine = WorkflowV2ExecutionEngine.getInstance();
        this.workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.actionCompletionGateway = AsyncActionCompletionGateway.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Workflow V2: Processed email action for Request ID: {}", streamMessage.id());

        final JsonObject dataJson = Json.object(streamMessage.data());

        if (dataJson.getBoolean(FieldKeys.SUCCESS.getName())) {
            final JsonObject stateJson = dataJson.getJsonObject(WorkflowAttributesV2.STATE);
            final JsonObject jsonObject = stateJson.getJsonObject(WorkflowAttributesV2.WORKFLOW_CONFIGURATION);

            final WorkflowEmailTracker workflowEmailTracker = new WorkflowEmailTracker();
            final long orgId = jsonObject.getLong(WorkflowAttributesV2.ORG_ID);
            final long executionId = jsonObject.getLong(WorkflowAttributesV2.EXECUTION_ID);
            final long nodeId = jsonObject.getLong(WorkflowAttributesV2.NODE_ID);
            final long conversationId = dataJson.getLong(WorkflowAttributesV2.CONVERSATION_ID);
            final long threadId = dataJson.getLong(WorkflowAttributesV2.THREAD_ID);

            workflowEmailTracker.setOrgId(orgId);
            workflowEmailTracker.setUserId(jsonObject.getLong(WorkflowAttributesV2.USER_ID));
            workflowEmailTracker.setExecutionId(executionId);
            workflowEmailTracker.setActionId(jsonObject.getLong(WorkflowAttributesV2.ACTION_ID));
            workflowEmailTracker.setNodeId(nodeId);
            workflowEmailTracker.setNodeType(jsonObject.getShort(WorkflowAttributesV2.NODE_TYPE));
            workflowEmailTracker.setThreadId(threadId);
            workflowEmailTracker.setConversationId(conversationId);

            final JsonObject json = Json.object();

            if (dataJson.containsKey(WorkflowAttributesV2.TRACKING)) {
                final List<Long> trackerIds = new ArrayList<>();

                dataJson.getJsonArray(WorkflowAttributesV2.TRACKING).forEach(trackerId -> trackerIds.add((Long) trackerId));
                workflowEmailTracker.setTrackerIds(trackerIds);
                json.put(WorkflowAttributesV2.TRACKING, trackerIds);
            }
            json.put(WorkflowAttributesV2.THREAD_ID, threadId);

            workflowEmailTrackerManager.register(workflowEmailTracker);
            executionManager.upsertSourceEntity(new ExecutionEntity(orgId, executionId, nodeId, conversationId, json, ExecutionEntity.ExecutionSource.EMAIL));
            actionCompletionGateway.completeAsyncAction(orgId, executionId, nodeId, null);
        } else if (dataJson.containsKey(CoreEmailFields.INITIAL_MESSAGE.getName())) {
            final long threadId = dataJson.getLong(CoreEmailFields.THREAD_ID.getName());
            final Optional<WorkflowEmailTracker> optionalWorkflowEmailTracker = workflowEmailTrackerManager.getByThreadId(threadId);

            optionalWorkflowEmailTracker.ifPresent(workflowEmailTracker ->
                    executor.execute(() -> executionEngine.tryEmailResumeWait(workflowEmailTracker)));
        } else if (dataJson.containsKey(CoreEmailFields.IS_CLICKED.getName())) {
            final long conversationId = dataJson.getLong(CoreEmailFields.CONVERSATION_ID.getName());
            final Optional<WorkflowEmailTracker> optionalWorkflowEmailTracker = workflowEmailTrackerManager.getByConversationId(conversationId);

            optionalWorkflowEmailTracker.ifPresent(workflowEmailTracker ->
                    executor.execute(() -> executionEngine.tryEmailResumeWait(workflowEmailTracker)));
        } else {
            final JsonObject stateJson = dataJson.getJsonObject(WorkflowAttributesV2.STATE);
            final JsonObject jsonObject = stateJson.getJsonObject(WorkflowAttributesV2.WORKFLOW_CONFIGURATION);

            actionCompletionGateway.failAsyncAction(jsonObject.getLong(WorkflowAttributesV2.ORG_ID), jsonObject.getLong(WorkflowAttributesV2.EXECUTION_ID),
                    jsonObject.getLong(WorkflowAttributesV2.NODE_ID), WorkflowActionExecutionFailureCause.UNIDENTIFIED_ERROR.getId());
        }
    }
}

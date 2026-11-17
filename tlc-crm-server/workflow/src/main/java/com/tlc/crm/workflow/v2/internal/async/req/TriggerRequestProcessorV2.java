package com.tlc.crm.workflow.v2.internal.async.req;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.models.Action;

import com.tlc.crm.core.workflow.TriggerDataConverter;
import com.tlc.crm.core.workflow.model.ModelSource;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.internal.execution.TriggerExecutionData;
import com.tlc.crm.workflow.v2.internal.execution.TriggerProcessorV2;
import com.tlc.crm.workflow.v2.internal.execution.WorkflowV2ExecutionEngine;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import com.tlc.validator.TlcModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * V2 trigger Kafka consumer: resumes matching waits and starts published workflows.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class TriggerRequestProcessorV2 implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerRequestProcessorV2.class);

    private final TriggerDataConverter triggerDataConverter;
    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowV2ExecutionEngine workflowV2ExecutionEngine;
    private final Executor triggerExecutor;
    private final Executor waitMatcherExecutor;
    private final int waitMatcherBatchSize;

    public TriggerRequestProcessorV2(final Executor triggerExecutor, final Executor waitMatcherExecutor, final int waitMatcherBatchSize) {
        this.triggerDataConverter = TriggerDataConverter.getInstance();
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.workflowV2ExecutionEngine = WorkflowV2ExecutionEngine.getInstance();
        this.triggerExecutor = triggerExecutor;
        this.waitMatcherExecutor = waitMatcherExecutor;
        this.waitMatcherBatchSize = waitMatcherBatchSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Workflow V2 Execution: Trigger request received by the consumer. Id : {}", streamMessage.id());

        final String triggerRequestId = streamMessage.id();
        final String[] decodedParts = new String(Base64.getDecoder().decode(triggerRequestId.getBytes())).split(":");
        final long orgId = Long.parseLong(decodedParts[0]);
        final Source source = Source.get(decodedParts[1]);
        final long userId = Long.parseLong(decodedParts[3]);
        final JsonObject jsonObject = Json.object(streamMessage.data());
        final Action action = Action.valueOf(jsonObject.getString(WorkflowAttributes.ACTION));
        final TriggerSource triggerSource = TriggerSource.getByName(source.getName());
        final TriggerEvent triggerEvent = switch (action) {
            case CREATE -> TriggerEvent.CREATED;
            case UPDATE, PARTIAL_UPDATE -> TriggerEvent.EDITED;
            case DELETE -> TriggerEvent.DELETED;
            default ->
                    throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event");
        };
        final long currentTime = System.currentTimeMillis();

        if (workflowV2Manager.exists(orgId, triggerSource, triggerEvent)) {
            triggerExecutor.execute(() -> {
                try {
                    LOGGER.debug("Workflow V2 Execution: Fetching model details for request Id: {}. Org Id: {}, Source: {}, Action: {}",
                            triggerRequestId, orgId, source, action);
                    final Map<Long, Long> entityStateIdMap = new HashMap<>();
                    final Collection<Pair<TlcModel, TlcModel>> modelPairs = triggerDataConverter.extractModelPairs(jsonObject, source, action, entityStateIdMap);
                    final JsonObject nestedTriggerInfo = jsonObject.optJsonObject(WorkflowAttributes.NESTED_TRIGGER_INFO);

                    LOGGER.debug("Workflow V2 Execution: Fetched model pairs for request Id: {} in {} ms. Org Id: {}, Source: {}, Action: {}",
                            triggerRequestId, System.currentTimeMillis() - currentTime, orgId, source, action);

                    final Collection<WorkflowV2> workflows = workflowV2Manager.getWorkflowsByTrigger(orgId, triggerSource, triggerEvent);
                    final TriggerExecutionData triggerExecutionData = new TriggerExecutionData(orgId, modelPairs,
                            triggerSource, ModelSource.valueOf(jsonObject.getString(WorkflowAttributes.ORIGIN)),
                            jsonObject.optBoolean(WorkflowAttributes.NEED_NESTED_TRIGGER), unused -> streamMessage.clean(),
                            entityStateIdMap, nestedTriggerInfo);

                    if (triggerEvent == TriggerEvent.EDITED) {
                        waitMatcherExecutor.execute(() -> workflowV2ExecutionEngine.resumeMatchingWaits(triggerExecutionData, waitMatcherBatchSize));
                    }

                    TriggerProcessorV2.get(triggerSource).handle(userId, triggerExecutionData, workflows);
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Workflow V2 Execution: Wait matching failed for request Id: {}. Org Id: {}, Source: {}, Action: {}, Reason: {}",
                            triggerRequestId, orgId, source, action, throwable.getMessage());
                } finally {
                    streamMessage.clean();
                    LOGGER.info("Workflow Execution: Successfully completed the trigger request for Id: {} in {} ms", triggerRequestId, System.currentTimeMillis() - currentTime);
                }
            });
        } else {
            streamMessage.clean();
            LOGGER.info("Workflow V2 Execution: No matching V2 workflow for request Id: {} in {} ms", triggerRequestId, System.currentTimeMillis() - currentTime);
        }
    }
}

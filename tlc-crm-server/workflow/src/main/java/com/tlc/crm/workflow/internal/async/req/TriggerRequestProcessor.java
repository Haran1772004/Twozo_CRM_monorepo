package com.tlc.crm.workflow.internal.async.req;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.validator.TlcModel;

import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.core.workflow.model.ModelSource;
import com.tlc.crm.core.workflow.TriggerDataConverter;

import com.tlc.crm.workflow.api.manager.WorkflowManager;
import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.api.model.TriggerSource.TriggerEvent;
import com.tlc.crm.workflow.field.WorkflowAttributes;
import com.tlc.crm.workflow.internal.trigger.model.TriggerContext;
import com.tlc.crm.workflow.internal.trigger.manager.TriggerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A consumer that consumes the trigger requests and triggers the various actions
 * based on the request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerRequestProcessor.class);

    private final TriggerDataConverter triggerDataConverter;
    private final WorkflowManager workflowManager;
    private final Executor triggerExecutor;

    public TriggerRequestProcessor(final Executor triggerExecutor) {
        this.triggerDataConverter = TriggerDataConverter.getInstance();
        this.workflowManager = WorkflowManager.getInstance();
        this.triggerExecutor = triggerExecutor;
    }

    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Workflow Execution: Trigger request received by the consumer. Id : {}", streamMessage.id());

        final String triggerRequestId = streamMessage.id();
        final String[] decodedParts = new String(Base64.getDecoder().decode(triggerRequestId.getBytes())).split(":");
        final long orgId = Long.parseLong(decodedParts[0]);
        final Source source = Source.get(decodedParts[1]);
        final JsonObject jsonObject = Json.object(streamMessage.data());
        final Action action = Action.valueOf(jsonObject.getString(WorkflowAttributes.ACTION));
        final TriggerSource triggerSource = TriggerSource.getByName(source.getName());
        final TriggerEvent triggerEvent = switch (action) {
            case CREATE -> TriggerEvent.CREATED;
            case UPDATE, PARTIAL_UPDATE -> TriggerEvent.EDITED;
            case DELETE -> TriggerEvent.DELETED;
            default -> throw ErrorCode.get( WorkflowErrorCodes.INVALID_TRIGGER_EVENT, "i18n.workflow.invalid.trigger.event" );
        };
        final long currentTime = System.currentTimeMillis();

        if (workflowManager.exists(orgId, triggerSource, triggerEvent)) {

            triggerExecutor.execute(() -> {

                try {
                    LOGGER.info("Workflow Execution: Going to fetch model details for received trigger request Id: {}. Org Id: {}, Source: {}, Action: {}", triggerRequestId, orgId, source, action);
                    final Map<Long, Long> entityStateIdMap = new HashMap<>();
                    final Collection<Pair<TlcModel, TlcModel>> modelPairs = triggerDataConverter.extractModelPairs(jsonObject, source, action, entityStateIdMap);

                    LOGGER.info("Workflow Execution: Successfully fetched the model pairs for the trigger request Id: {} in {} ms. Org Id: {}, Source: {}, Action: {}", triggerRequestId, System.currentTimeMillis() - currentTime, orgId, source, action);
                    final JsonObject nestedTriggerInfo = jsonObject.optJsonObject(WorkflowAttributes.NESTED_TRIGGER_INFO);
                    final TriggerContext triggerContext = new TriggerContext<>(orgId, modelPairs,
                            triggerSource, ModelSource.valueOf(jsonObject.getString(WorkflowAttributes.ORIGIN)),
                            jsonObject.optBoolean(WorkflowAttributes.NEED_NESTED_TRIGGER), unused -> streamMessage.clean(),
                            entityStateIdMap, nestedTriggerInfo);

                    final Collection<Workflow> workflows = workflowManager.getWorkflowByTrigger(orgId, triggerSource, triggerEvent);

                    switch (action) {
                        case CREATE -> TriggerManager.get(triggerSource).triggerCreateEvent(triggerContext, workflows);
                        case UPDATE -> TriggerManager.get(triggerSource).triggerUpdateEvent(triggerContext, workflows);
                        case DELETE -> TriggerManager.get(triggerSource).triggerDeleteEvent(triggerContext, workflows);
                    }
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Workflow Execution: Trigger action failed for request Id: {}. Org Id: {}, Source: {}, Action: {}, Reason: {}", triggerRequestId, orgId, source, action, throwable.getMessage());
                } finally {
                    streamMessage.clean();
                    LOGGER.info("Workflow Execution: Successfully completed the trigger request for Id: {} in {} ms", triggerRequestId, System.currentTimeMillis() - currentTime);
                }
            });
        } else {
            streamMessage.clean();
            LOGGER.info("Workflow Execution: Trigger request completed for Id: {} in {} ms", triggerRequestId, System.currentTimeMillis() - currentTime);
        }
    }
}

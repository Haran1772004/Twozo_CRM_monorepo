package com.tlc.crm.workflow.internal.async.req;

import com.tlc.commons.json.Json;

import com.tlc.crm.workflow.internal.trigger.action.ActionDataProcessor;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.ActionDistributor;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * A consumer that consumes the workflow action requests based on the request parameters.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionRequestProcessor.class);

    private final ActionDistributor actionDistributor;
    private final ActionDataProcessor actionDataProcessor;

    public ActionRequestProcessor() {
        this.actionDistributor = ActionDistributor.getInstance();
        this.actionDataProcessor = ActionDataProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Holds the action request parameters.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final String requestId = streamMessage.id();
        final String[] decodedParts = new String(Base64.getDecoder().decode(requestId.getBytes())).split(":");
        final long orgId = Long.parseLong(decodedParts[0]);
        final long userId = Long.parseLong(decodedParts[1]);

        LOGGER.info("Workflow Execution: Action request received by the consumer. Request Id : {}, Org Id: {}, User Id: {}", requestId, orgId, userId);
        final ActionExecutionContext actionExecutionContext = actionDataProcessor.unwrapData(Json.object(streamMessage.data()));

        actionDistributor.distribute(orgId, userId, actionExecutionContext);
    }
}

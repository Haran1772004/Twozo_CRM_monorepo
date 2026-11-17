package com.tlc.crm.email.core.consumer;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.email.core.handler.EmailAddOnServiceDataLoadActionHandler;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * A listener that processes inbound email.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InboundEmailDataLoadActionListener implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEmailDataLoadActionListener.class);

    /**
     * <p>
     * Default constructor for {@link InboundEmailDataLoadActionListener}.
     * </p>
     */
    public InboundEmailDataLoadActionListener() {
    }

    /**
     * <p>
     * Invoked whenever an event is received by the consumer. Decodes the
     * stream message into a {@link ThreadHolder} and processes it.
     * </p>
     *
     * @param streamMessage Represents the message consumed from the stream
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final ThreadCodec threadCodec = (ThreadCodec) EmailCodec.get(EmailCodec.CodecType.THREAD_CONFIG);
            final ThreadHolder threadHolder = threadCodec.decode(requestParameters.getBytes());

            if (Objects.nonNull(threadHolder)) {
                LOGGER.debug("Inbound email load: Listener consumed, Sync Address: {}",
                        threadHolder.getSyncedEmailAddress().getEmailAddress());

                loadEmail(threadHolder);
            }
        } catch (Throwable throwable) {
            LOGGER.debug("Inbound email load: Failure while loading inbound email load action: {}", throwable.getMessage());
        }
    }

    /**
     * <p>
     * Delegates processing of the decoded {@link ThreadHolder} to the handler
     * </p>
     *
     * @param threadHolder Represents the decoded email thread to be processed
     */
    private void loadEmail(final ThreadHolder threadHolder) {
        final StateData stateData = threadHolder.getStateData();

        if (Objects.nonNull(stateData)) {
            EmailAddOnServiceDataLoadActionHandler
                    .get(ServiceType.get(Objects.requireNonNull(stateData.getString(EmailFields.SERVICE_TYPE))))
                    .handle(threadHolder);
        }
    }
}
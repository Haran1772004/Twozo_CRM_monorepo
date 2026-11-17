package com.tlc.email.common.publish;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.UpdateRequestCodec;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * Responsible for publishing email update requests. And to handle and propagate update request messages
 * to the appropriate email processing service.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailRequestPublisher
 */
public class EmailUpdateRequestPublisher extends AbstractEmailRequestPublisher<UpdateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailUpdateRequestPublisher.class);

    private final EmailKafkaProducer emailKafkaProducer;

    protected EmailUpdateRequestPublisher() {
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
    }

    /**
     * <p>
     * Holds the single instance.
     * </p>
     */
    private static class Instance {
        private static final EmailUpdateRequestPublisher INSTANCE = new EmailUpdateRequestPublisher();
    }

    public static EmailUpdateRequestPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param updateRequest the email update request to provider to be published
     */
    @Override
    public void publish(final UpdateRequest updateRequest) {

        try {
            final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getUpdateRequestProviderTopic(QueueModel.Type.KAFKA);
            final String requestId = Base64.getEncoder().encodeToString(String.format("%s:",
                    updateRequest.getSyncEmailAddress().getEmailAddress()).getBytes());
            final UpdateRequestCodec updateRequestCodec = (UpdateRequestCodec) EmailCodec.get(EmailCodec.CodecType.UPDATE);
            final Producer<byte[]> emailSyncProducer = emailKafkaProducer.getProducer(subscriptionTopic);

            LOGGER.debug("Email Update Action: Publishes the update request action");
            emailSyncProducer.send(new StreamMessage<>(requestId, subscriptionTopic, updateRequestCodec.encode(updateRequest)));
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Update Action: Update request publish action failed. message: {}, trace: {}",
                    throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.PROVIDER_UPDATE_PUBLISH_ACTION_FAILED, throwable);
        }
    }
}


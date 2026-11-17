package com.tlc.email.common.publish;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.codec.DeleteRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.DeleteRequest;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * Responsible for publishing email delete requests. And to handle and propagate delete request messages
 * to the appropriate email processing service.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailRequestPublisher
 */
public class EmailDeleteRequestPublisher extends AbstractEmailRequestPublisher<DeleteRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDeleteRequestPublisher.class);

    private final EmailKafkaProducer emailKafkaProducer;

    protected EmailDeleteRequestPublisher() {
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
    }

    /**
     * <p>
     * Holds the single instance.
     * </p>
     */
    private static class Instance {
        private static final EmailDeleteRequestPublisher INSTANCE = new EmailDeleteRequestPublisher();
    }

    public static EmailDeleteRequestPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param deleteRequest the email delete request to provider to be published
     */
    @Override
    public void publish(final DeleteRequest deleteRequest) {

        try {
            final String requestId = Base64.getEncoder().encodeToString(String.format("%s:",
                    deleteRequest.getSyncEmailAddress().getEmailAddress()).getBytes());
            final DeleteRequestCodec deleteRequestCodec = (DeleteRequestCodec) EmailCodec.get(EmailCodec.CodecType.DELETE);
            final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDeleteRequestProviderTopic(QueueModel.Type.KAFKA);
            final Producer<byte[]> emailSyncProducer = emailKafkaProducer.getProducer(subscriptionTopic);

            LOGGER.debug("Email Delete Action: Publishes the delete request action");
            emailSyncProducer.send(new StreamMessage<>(requestId, subscriptionTopic, deleteRequestCodec.encode(deleteRequest)));
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Delete Action: Delete request publish action failed. message: {}, trace: {}",
                    throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.PROVIDER_DELETE_PUBLISH_ACTION_FAILED, throwable);
        }
    }
}


package com.tlc.email.common.publish;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.codec.TransactionalMailCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.SendRequestCodec;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.SendRequest;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * <p>
 * Publishes the {@link SendRequest}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailRequestPublisher
 */
public class EmailSendRequestPublisher extends AbstractEmailRequestPublisher<SendRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendRequestPublisher.class);

    private final EmailKafkaProducer emailKafkaProducer;

    private EmailSendRequestPublisher() {
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
    }

    /**
     * <p>
     * Holds the single instance.
     * </p>
     */
    private static class Instance {
        private static final EmailSendRequestPublisher INSTANCE = new EmailSendRequestPublisher();
    }

    public static EmailSendRequestPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param sendRequest the email send request to provider to be published
     */
    @Override
    public void publish(final SendRequest sendRequest) {
        final Email email = sendRequest.getEmail();
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:", email.getSender().getEmailAddress()).getBytes());
        final QueueModel.Type queueModelType = QueueModel.Type.KAFKA;

        try {
            final byte[] data;
            final SubscriptionTopic subscriptionTopic;

            if (sendRequest.isTransactionalMail()) {
                subscriptionTopic = EmailQueueTopics.getTransactionalMailTopic(queueModelType);
                data = ((TransactionalMailCodec) EmailCodec.get(EmailCodec.CodecType.TRANSACTIONAL)).encode(sendRequest);
            } else {
                subscriptionTopic = EmailQueueTopics.getSendRequestProviderTopic(queueModelType);
                data = ((SendRequestCodec) EmailCodec.get(EmailCodec.CodecType.SEND)).encode(sendRequest);
            }
            final Producer<byte[]> emailSyncProducer = emailKafkaProducer.getProducer(subscriptionTopic);

            LOGGER.debug("Email Send Action: Publishes the send request");

            emailSyncProducer.send(new StreamMessage<>(requestId, subscriptionTopic, data));
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Send Action: Send request publish action failed. message: {}, trace: {}",
                    throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.PROVIDER_SEND_PUBLISH_ACTION_FAILED, throwable);
        }
    }
}

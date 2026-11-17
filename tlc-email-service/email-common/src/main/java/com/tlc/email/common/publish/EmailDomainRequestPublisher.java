package com.tlc.email.common.publish;

import com.tlc.email.common.codec.EmailDomainRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * <p>
 * Publishes domain-related requests to the configured queue system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailDomainRequestPublisher extends AbstractEmailRequestPublisher<EmailDomainRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDomainRequestPublisher.class);

    private final EmailKafkaProducer emailKafkaProducer;

    /**
     * <p>
     * Private constructor to enforce singleton usage.
     * </p>
     */
    private EmailDomainRequestPublisher() {
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link EmailDomainRequestPublisher}.
     * </p>
     */
    private static class Instance {
        private static final EmailDomainRequestPublisher INSTANCE = new EmailDomainRequestPublisher();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link EmailDomainRequestPublisher}.
     * </p>
     *
     * @return The singleton {@link EmailDomainRequestPublisher} instance.
     */
    public static EmailDomainRequestPublisher getInstance() {
        return EmailDomainRequestPublisher.Instance.INSTANCE;
    }

    /**
     * <p>
     * Publishes the provided {@link EmailDomainRequest} to the configured domain action queue.
     * </p>
     *
     * @param emailDomainRequest The {@link EmailDomainRequest} to publish.
     */
    @Override
    public void publish(final EmailDomainRequest emailDomainRequest) {
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:", emailDomainRequest.getDomain().getName()).getBytes());
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDomainRequestToProviderTopic(QueueModel.Type.KAFKA);
        final Producer<byte[]> emailSyncProducer = emailKafkaProducer.getProducer(subscriptionTopic);

        final byte[] data = ((EmailDomainRequestCodec) EmailCodec.get(EmailCodec.CodecType.DOMAIN))
                .encode(emailDomainRequest);

        emailSyncProducer.send(new StreamMessage<>(requestId, subscriptionTopic, data));

        LOGGER.debug("Email Domain Action: Publishes the domain request");
    }
}
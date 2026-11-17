package com.tlc.email.common.publish;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.Collection;

/**
 * <p>
 * Abstract implementation for publishing email sync requests to Kafka,
 * with support for batching and error handling.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public abstract class AbstractEmailRequestPublisher<T> implements EmailRequestPublisher<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEmailRequestPublisher.class);

    protected final EmailKafkaProducer emailKafkaProducer;

    protected AbstractEmailRequestPublisher() {
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
    }

    /**
     * <p>
     * Publishes a basic email sync request (overloaded method for generic requests).
     * </p>
     *
     * @param request the email sync request to be published
     */
    @Override
    public void publish(final T request) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_PUBLISH_ACTION);
    }

    /**
     * <p>
     * Publishes an email sync request along with a collection of email threads.
     * </p>
     *
     * @param request the email request object to be published
     * @param threads a collection of email threads associated with the request
     */
    @Override
    public void publish(final T request, final Collection<Thread> threads) {
        throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_PUBLISH_ACTION);
    }

    /**
     * <p>
     * Publishes an email sync request with the provided sync ID, email address, encoded data,
     * and subscription topic to Kafka.
     * </p>
     *
     * @param emailAddress      the email address associated with the sync
     * @param encodedData       the encoded data to be sent
     * @param subscriptionTopic the subscription topic for Kafka
     */
    protected void publish(final String emailAddress, byte[] encodedData, final SubscriptionTopic subscriptionTopic) {
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:", emailAddress).getBytes());

        try {
            final Producer<byte[]> emailSyncProducer = emailKafkaProducer.getProducer(subscriptionTopic);

            emailSyncProducer.send(new StreamMessage<>(requestId, subscriptionTopic, encodedData));
        } catch (final Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Sync: Email request publish action failed. message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_PUBLISH_ACTION_FAILED, throwable);
        }
    }

    /**
     * <p>
     * Publishes an email sync request with sync ID, sync address info, threads, state data,
     * and subscription topic, while batching threads and handling encoding and publishing.
     * </p>
     *
     * @param syncAddressInfo   sync address information for the email sync
     * @param threads           the email threads to be published
     * @param stateData         the state data associated with the sync
     * @param subscriptionTopic the subscription topic for Kafka
     */
    protected void publish(final SyncAddressInfo syncAddressInfo, final Collection<Thread> threads,
                           final StateData stateData, final SubscriptionTopic subscriptionTopic) {
        final String requestId = Base64.getEncoder().encodeToString(syncAddressInfo.getEmailAddress().getBytes());

        try {
            final Producer<byte[]> producer = emailKafkaProducer.getProducer(subscriptionTopic);

            for (final Collection<Thread> batchedThreads : JavaUtil.splitCollection(threads, NumericalConstant.TEN)) {
                final ThreadCodec threadCodec = EmailCodec.get(EmailCodec.CodecType.THREAD_CONFIG).get();
                final ThreadHolder threadHolder = new ThreadHolder();

                threadHolder.setSyncedEmailAddress(syncAddressInfo);
                threadHolder.setThreads(batchedThreads);
                threadHolder.setStateData(stateData);

                producer.send(new StreamMessage<>(requestId, subscriptionTopic, threadCodec.encode(threadHolder)));
            }

            LOGGER.debug("Email Sync: Publishes the threads to other services, time: {}", Instant.now().toEpochMilli());
        } catch (Throwable throwable) {
            LoggerUtil.logError(LOGGER, throwable, "Email Sync: Threads publish action failed. message: {}, trace: {}", throwable.getMessage(), throwable);

            throw ErrorCode.get(EmailErrorCodes.EMAIL_LOAD_PUBLISH_ACTION_FAILED, throwable);
        }
    }
}
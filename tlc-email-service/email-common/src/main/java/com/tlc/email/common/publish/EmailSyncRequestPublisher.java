package com.tlc.email.common.publish;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.SyncRequestCodec;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;

import java.util.Collection;

/**
 * <p>
 * Publishes the sync request and associated threads for email synchronization.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailSyncRequestPublisher extends AbstractEmailRequestPublisher<SyncRequest> {

    private EmailSyncRequestPublisher() {
    }

    /**
     * <p>
     * Holds the single instance.
     * </p>
     */
    private static class Instance {
        private static final EmailSyncRequestPublisher INSTANCE = new EmailSyncRequestPublisher();
    }

    public static EmailSyncRequestPublisher getInstance() {
        return EmailSyncRequestPublisher.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param syncRequest Represents the {@link SyncRequest} to be published
     */
    @Override
    public void publish(final SyncRequest syncRequest) {
        final SyncRequestCodec syncRequestCodec = (SyncRequestCodec) EmailCodec.get(EmailCodec.CodecType.RECEIVE);
        final String emailAddress = syncRequest.getReceiverConfiguration().getSyncEmailAddress().getEmailAddress();
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDataRetrieveTopic(QueueModel.Type.KAFKA, SyncJobType.SYNC);
        final byte[] encodedData = syncRequestCodec.encode(syncRequest);

        publish(emailAddress, encodedData, subscriptionTopic);
    }

    /**
     * {@inheritDoc}
     *
     * @param syncRequest Represents the {@link SyncRequest}
     * @param threads     Represents the {@link Collection} of {@link Thread}
     */
    @Override
    public void publish(final SyncRequest syncRequest, final Collection<Thread> threads) {
        final SyncAddressInfo syncAddressInfo = syncRequest.getReceiverConfiguration().getSyncEmailAddress();
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDataLoadTopic(QueueModel.Type.KAFKA, SyncJobType.SYNC);

        publish(syncAddressInfo, threads, syncRequest.getStateData(), subscriptionTopic);
    }
}
package com.tlc.email.common.publish;

import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.TenantRequestCodec;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.TenantSyncRequest;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;

import java.util.Collection;

/**
 * <p>
 * Publishes the tenant sync request and associated threads for email synchronization.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailTenantSyncRequestPublisher extends AbstractEmailRequestPublisher<TenantSyncRequest> {

    private EmailTenantSyncRequestPublisher() {
    }

    /**
     * <p>
     * Holds the single instance of email service.
     * </p>
     */
    private static class Instance {
        private static final EmailTenantSyncRequestPublisher INSTANCE = new EmailTenantSyncRequestPublisher();
    }

    public static EmailTenantSyncRequestPublisher getInstance() {
        return EmailTenantSyncRequestPublisher.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param tenantSyncRequest Represents the {@link TenantSyncRequest} to be published
     */
    @Override
    public void publish(final TenantSyncRequest tenantSyncRequest) {
        final TenantRequestCodec tenantRequestCodec = (TenantRequestCodec) EmailCodec.get(EmailCodec.CodecType.TENANT);
        final String emailAddress = tenantSyncRequest.getTenantConfiguration().getSyncEmailAddress().getEmailAddress();
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDataRetrieveTopic(QueueModel.Type.KAFKA, SyncJobType.SMART_BCC);
        final byte[] encodedData = tenantRequestCodec.encode(tenantSyncRequest);

        publish(emailAddress, encodedData, subscriptionTopic);
    }

    /**
     * {@inheritDoc}
     *
     * @param tenantSyncRequest Represents the {@link TenantSyncRequest}
     * @param threads           Represents the {@link Collection} of {@link Thread}
     */
    @Override
    public void publish(final TenantSyncRequest tenantSyncRequest, Collection<Thread> threads) {
        final SyncAddressInfo syncAddressInfo = tenantSyncRequest.getTenantConfiguration().getSyncEmailAddress();
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getDataLoadTopic(QueueModel.Type.KAFKA, SyncJobType.SMART_BCC);

        publish(syncAddressInfo, threads, tenantSyncRequest.getStateData(), subscriptionTopic);
    }
}
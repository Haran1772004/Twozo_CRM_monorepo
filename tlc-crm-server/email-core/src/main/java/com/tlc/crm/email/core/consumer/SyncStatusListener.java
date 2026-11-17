package com.tlc.crm.email.core.consumer;

import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.crm.security.context.SecureTaskProcessor;
import com.tlc.email.common.codec.SyncStatusCodec;
import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event handler that process the sync status requests.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see StreamListener
 */
public class SyncStatusListener implements StreamListener<byte[]>, SecureTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncStatusListener.class);

    private final SyncStatusCodec syncStatusCodec;
    private final EmailSyncSettingsService emailSyncSettingsService;

    public SyncStatusListener() {
        this.syncStatusCodec = SyncStatusCodec.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final SyncStatusInfo syncStatusInfo = syncStatusCodec.decode(streamMessage.data());
        final String emailAddress = syncStatusInfo.getEmailAddress();

        emailSyncSettingsService.update(syncStatusInfo);

        LOGGER.debug("Email Sync Status: Sync status updated for email address: {}", emailAddress);
    }
}

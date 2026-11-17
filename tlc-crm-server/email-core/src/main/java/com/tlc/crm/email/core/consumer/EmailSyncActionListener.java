package com.tlc.crm.email.core.consumer;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;

import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.email.core.fields.EmailLinkerFields;

import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncConverter;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.ThreadHolder;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * An event handler that process the email background sync requests and triggers the email sync action based on the request parameters
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSyncActionListener implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSyncActionListener.class);

    private final EmailSyncConverter emailSyncConverter;
    private final EmailLinkerService emailLinkerService;

    public EmailSyncActionListener() {
        this.emailSyncConverter = EmailSyncConverter.getInstance();
        this.emailLinkerService = EmailLinkerService.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject requestParameters = Json.object(streamMessage.data());
        final ThreadCodec threadCodec = (ThreadCodec) EmailCodec.get(EmailCodec.CodecType.THREAD_CONFIG);
        final ThreadHolder threadHolder = threadCodec.decode(requestParameters.getBytes());

        if (Objects.nonNull(threadHolder)) {
            final JsonObject data = threadHolder.getStateData().toJson();
            final long orgId = data.getLong(EmailLinkerFields.ORG_ID);
            final long userId = data.getLong(EmailLinkerFields.USER_ID);

            LOGGER.debug("Email Sync: Sync listener consumed successfully, Org Id: {}, User ID: {}", orgId, userId);

            try {
                final Collection<EmailSyncSettings> syncSettings = List.of(emailSyncConverter.convert(orgId, userId, data));

                emailLinkerService.loadRetrievedEmails(SyncJobType.SYNC, syncSettings, threadHolder);
            } catch (Throwable throwable) {
                LOGGER.debug("Email Sync: Email thread load action failed, Org ID: {}, User ID: {}, Reason: {}", orgId, userId, throwable.getMessage());
            }
        }
    }
}
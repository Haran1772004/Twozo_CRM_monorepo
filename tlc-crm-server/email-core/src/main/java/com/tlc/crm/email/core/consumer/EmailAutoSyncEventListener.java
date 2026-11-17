package com.tlc.crm.email.core.consumer;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.email.core.handler.AbstractWebhookEventHandler;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.EmailEventRequestCodec;
import com.tlc.email.common.model.AbstractEmailEvent;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.codec.EmailCodec.CodecType;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Email webhook event listener that processes incoming Kafka stream messages for deleted and updated email events.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class EmailAutoSyncEventListener extends AbstractWebhookEventHandler implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailAutoSyncEventListener.class);

    private final EmailLinkerService emailLinkerService;
    private final EmailSyncSettingsService emailSyncSettingsService;

    public EmailAutoSyncEventListener() {
        this.emailLinkerService = EmailLinkerService.getInstance();
        this.emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    /**
     * <p>
     * Invoked when a stream message is received. It checks for 'deleted' and 'updated' events and processes them accordingly.
     * </p>
     *
     * @param streamMessage the incoming Kafka stream message as byte array
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {

        try {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final EmailEventRequestCodec emailEventRequestCodec = (EmailEventRequestCodec) EmailCodec.get(CodecType.EMAIL_EVENT);
            final EmailEventRequest emailEventRequest = emailEventRequestCodec.decode(requestParameters.getBytes());

            if (Objects.nonNull(emailEventRequest)) {
                final AbstractEmailEvent createEvent = emailEventRequest.getCreateEvent();
                final AbstractEmailEvent updateEvent = emailEventRequest.getUpdateEvent();
                final EmailDeleteEvent deleteEvent = emailEventRequest.getDeleteEvent();

                if (Objects.nonNull(updateEvent)) {
                    final ThreadHolder threadHolder = updateEvent.getThreadHolder();

                    if (Objects.nonNull(threadHolder)) {
                        final Collection<Thread> threads = threadHolder.getThreads();

                        if (Objects.nonNull(threads) && !threads.isEmpty()) {
                            handleUpdateEvent(threads);
                        }
                    }
                }

                if (Objects.nonNull(createEvent)) {
                    final ThreadHolder threadHolder = createEvent.getThreadHolder();

                    if (Objects.nonNull(threadHolder)) {
                        final Collection<EmailSyncSettings> syncSettings = emailSyncSettingsService
                                .getSyncSettings(threadHolder.getSyncedEmailAddress().getEmailAddress());

                        emailLinkerService.loadRetrievedEmails(SyncJobType.AUTO_SYNC, syncSettings, threadHolder);
                    }
                }

                if (Objects.nonNull(deleteEvent)) {
                    final Collection<Long> ids = deleteEvent.ids();

                    if (Objects.nonNull(ids) && !ids.isEmpty()) {
                        handleDeleteEvent(ids);
                    }
                }
            }
        } catch (Throwable throwable) {
            LOGGER.debug("Email Webhook event: Email webhook event data consume action failed, Message: {}", throwable.getMessage());
        }
    }
}
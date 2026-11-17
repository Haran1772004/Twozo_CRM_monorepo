package com.tlc.crm.email.core.consumer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;
import com.tlc.crm.core.notification.email.fields.CoreEmailFields;
import com.tlc.crm.core.notification.email.internal.ScheduledEmailService;

import com.tlc.crm.core.notification.email.internal.status.EmailErrorCodes;
import com.tlc.crm.email.core.fields.EmailLinkerFields;
import com.tlc.crm.email.core.processor.BulkEmailLinkerProcessor;
import com.tlc.crm.email.core.processor.EmailLinkerProcessor;

import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

import static com.tlc.crm.email.notification.model.MessageDispatchType.BULK;

/**
 * <p>
 * An event handler that process the email send requests and triggers the email send action based on the request parameters
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSendActionListener implements StreamListener<byte[]>, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendActionListener.class);

    private final BulkEmailLinkerProcessor bulkEmailLinkerProcessor;
    private final EmailLinkerProcessor emailLinkerProcessor;
    private final ScheduledEmailService scheduledEmailService;
    private final EmailSchedulerManager emailSchedulerManager;

    public EmailSendActionListener() {
        this.bulkEmailLinkerProcessor = BulkEmailLinkerProcessor.getInstance();
        this.emailLinkerProcessor = EmailLinkerProcessor.getInstance();
        this.scheduledEmailService = ScheduledEmailService.getInstance();
        this.emailSchedulerManager = EmailSchedulerManager.getInstance();
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
            LOGGER.debug("Email Send Action: Send action listener consumed successfully");
            final Optional<Thread> optionalThread = threadHolder.getThreads().stream().findFirst();

            if (optionalThread.isPresent()) {
                final StateData stateData = threadHolder.getStateData();
                final JsonObject stateJson = stateData.toJson();
                final long orgId = stateJson.getLong(EmailLinkerFields.ORG_ID);
                final long userId = stateJson.getLong(EmailLinkerFields.USER_ID);
                final Thread thread = optionalThread.get();
                final JsonObject jsonData = stateJson.containsKey(EmailLinkerFields.DATA) ? stateJson.getJsonObject(EmailLinkerFields.DATA) : Json.emptyObject();
                final String conversationIdKey = CoreEmailFields.CONVERSATION_ID.getName();

                try {
                    if (stateJson.containsKey(EmailLinkerFields.IS_CANCELLED)) {
                        final Long conversationId = stateJson.getLong(conversationIdKey);
                        final MessageDispatchType messageDispatchType = MessageDispatchType.get(stateJson.getShort(CoreEmailFields.SCHEDULED_TYPE.getName()));
                        final Long mailId = BULK == messageDispatchType ? stateJson.getLong(CoreEmailFields.ID.getName()) : null;

                        secureProcess(orgId, userId, () -> scheduledEmailService.updateMailState(orgId, userId, mailId, conversationId, thread));
                    } else {

                        if (stateJson.containsKey(EmailLinkerFields.IS_BULK) && stateJson.containsKey(EmailLinkerFields.DATA)) {
                            bulkEmailLinkerProcessor.process(orgId, userId, thread, stateJson);
                        } else {
                            emailLinkerProcessor.process(orgId, userId, thread, jsonData, stateJson);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Email Send Action: Send action failed for the given OrgId: {}, User ID: {}", orgId, userId, e);

                    if (jsonData.optBoolean(CoreEmailFields.IS_SCHEDULED.getName()) && jsonData.containsKey(conversationIdKey)) {
                        secureProcess(orgId, userId, () -> emailSchedulerManager.updateState(orgId,
                                jsonData.getLong(conversationIdKey), EntityScheduledEmail.State.FAILED));
                    }
                    throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, e);
                }
            }
        }
    }
}

package com.tlc.crm.core.notification.email.internal.controller;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.notification.email.api.manager.schedule.EmailSchedulerManager;
import com.tlc.crm.core.notification.email.api.model.converter.EmailConverter;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityScheduledEmail;

import com.tlc.crm.email.notification.email.EmailDispatcher;

import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.common.model.type.Email;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.tlc.commons.util.constants.NumericalConstant.ZERO;

/**
 * <p>
 * A service that is responsible to control the scheduled email actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class ScheduledEmailServiceController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledEmailServiceController.class);
    private static final String THREAD_POOL_NAME = "scheduled-email-pool";
    private static final String TASK_SCHEDULER_NAME = "scheduled-email";
    private final SchedulerService schedulerService;
    private Long schedulerId;

    private static class Instance {
        private static final ScheduledEmailServiceController INSTANCE = new ScheduledEmailServiceController();
    }

    private ScheduledEmailServiceController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link ScheduledEmailServiceController} Instance
     * </p>
     *
     * @return {@link ScheduledEmailServiceController}
     */
    public static ScheduledEmailServiceController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new ScheduledEmailTask(new ScheduledEmailTaskController()));

            LOGGER.debug("Scheduled email service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);
            LOGGER.debug("Scheduled email service stopped Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to send the scheduled email activity
     * </p>
     *
     * @author Kavinila E
     * @version 1.0
     */
    private static class ScheduledEmailTask extends Task implements SecureUserTaskProcessor {

        private final EmailSchedulerManager emailSchedulerManager;
        private final EmailDispatcher emailDispatcher;
        private final ClusterService clusterService;

        private ScheduledEmailTask(final TaskController controller) {
            super(controller);
            this.emailSchedulerManager = EmailSchedulerManager.getInstance();
            this.emailDispatcher = EmailDispatcher.get();
            this.clusterService = ClusterServiceProvider.get();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("scheduledMail");

                    distributedLock.acquire(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
                    sendEmails();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.debug("Scheduled email sending action failed, Error : {}", e.getMessage());
                LOGGER.debug("Stack", e);
            }
        }

        /**
         * <p>
         * Sends the emails which are scheduled at the current time
         * </p>
         */
        private void sendEmails() {
            final long utcTime = Instant.now().toEpochMilli();

            if (this.emailSchedulerManager.getScheduledMailCount(utcTime) > ZERO) {
                final Map<EntityScheduledEmail, EntityOutboundEmail> scheduledMails = this.emailSchedulerManager.getEmailDataStore(utcTime);

                if (Objects.nonNull(scheduledMails)) {

                    for (final Map.Entry<EntityScheduledEmail, EntityOutboundEmail> entry : scheduledMails.entrySet()) {
                        final EntityScheduledEmail scheduledConversation = entry.getKey();
                        final EntityOutboundEmail outboundEmail = entry.getValue();
                        final long orgId = scheduledConversation.orgId();
                        final long userId = outboundEmail.getUserId();
                        final long conversationId = outboundEmail.getConversationId();

                        try {
                            secureProcess(orgId, userId, () -> {
                                final SyncAddressInfo emailAddress = outboundEmail.getSender();

                                if (Objects.equals(emailAddress.id(), scheduledConversation.getSender().id())) {
                                    this.emailSchedulerManager.updateState(orgId, conversationId, EntityScheduledEmail.State.QUEUE);
                                    this.sendEmail(outboundEmail, getStateData(outboundEmail, outboundEmail.isScheduled()));
                                } else {
                                    this.emailSchedulerManager.updateScheduledMailState(orgId, scheduledConversation.getCrmId());
                                }
                                LOGGER.debug("Scheduled Email: Send action completed, orgId:{}, userId:{}, conversationId:{}", orgId, userId, conversationId);
                            });
                        } catch (final Exception exception) {
                            LoggerUtil.logError(LOGGER, exception,
                                    "Scheduled Email: Send action failed, orgId: {}, userId: {}, conversationId: {}", orgId, userId, conversationId);
                            this.emailSchedulerManager.updateState(orgId, conversationId, EntityScheduledEmail.State.FAILED);
                        }
                    }
                }
            }
        }

        /**
         * <p>
         * Gets the state data for the provided outbound mail
         * </p>
         *
         * @param outboundEmail Represents the outbound mail to be sent
         * @param isScheduled   Represents whether it is scheduled or not
         * @return the JSON which contains the state data
         */
        private JsonObject getStateData(final EntityEmail outboundEmail, final boolean isScheduled) {
            final JsonObject stateData = Json.object();

            stateData.put(EmailFields.ORG_ID.getName(), outboundEmail.orgId());
            stateData.put(EmailFields.USER_ID.getName(), outboundEmail.getUserId());
            final JsonObject entityEmailData = EmailConverter.getInstance().convert(outboundEmail);

            entityEmailData.put(EmailFields.THREAD_ID.getName(), outboundEmail.getThreadId());
            entityEmailData.put(EmailFields.CONVERSATION_ID.getName(), outboundEmail.getConversationId());
            entityEmailData.put(EmailFields.IS_SCHEDULED.getName(), isScheduled);
            final EmailVariant emailVariant = outboundEmail.getEmailVariant();

            if (Objects.nonNull(emailVariant)) {
                entityEmailData.put(EmailFields.VARIANT.getName(), Json.object()
                        .put(EmailFields.TYPE.getName(), emailVariant.getValue()));
            }
            stateData.put(EmailFields.DATA.getName(), entityEmailData);

            return stateData;
        }

        /**
         * <p>
         * Sends the email to the provider.
         * </p>
         *
         * @param entityEmail Represents the {@link EntityEmail}
         */
        private void sendEmail(final EntityEmail entityEmail, final JsonObject stateData) {
            final Email email = entityEmail.getEmail();

            email.setMetaData(entityEmail.getMetaData());
            email.setSender(entityEmail.getSender());
            email.setSubject(entityEmail.getSubject());
            email.setContent(entityEmail.getContent());
            email.setRecipients(entityEmail.getRecipients());
            email.setAttachments(entityEmail.getAttachments());
            email.setFiles(entityEmail.getFiles());
            email.setId(entityEmail.id());

            emailDispatcher.send(email, stateData);
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls scheduled email task {@link ScheduledEmailTask}.
     *
     * @author Kavinila E
     * @version 1.0
     */
    public static class ScheduledEmailTaskController extends TaskController {

        public ScheduledEmailTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(5);
        }
    }
}

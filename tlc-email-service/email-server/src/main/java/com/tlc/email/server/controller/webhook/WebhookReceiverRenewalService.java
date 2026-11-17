package com.tlc.email.server.controller.webhook;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.server.internal.field.EmailKeys;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebHookReceiverManager;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebhookReceiverManagerImpl;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Collection;
import java.util.Objects;

/**
 * Service responsible for renewing webhook receiver subscriptions.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see Service
 */
public class WebhookReceiverRenewalService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookReceiverRenewalService.class);

    private static final String THREAD_POOL_NAME = "webhook-receiver-renewal-pool";
    private static final String TASK_SCHEDULER_NAME = "webhook-receiver-renewal";
    private static final String TASK_SCHEDULE_DELAY = "webhook.renewal.schedule.delay";

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private WebhookReceiverRenewalTask webhookReceiverRenewalTask;

    private WebhookReceiverRenewalService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link WebhookReceiverRenewalService}.
     * </p>
     */
    private static class Instance {
        private static final WebhookReceiverRenewalService INSTANCE = new WebhookReceiverRenewalService();
    }

    /**
     * <p>
     * Gets the {@link WebhookReceiverRenewalService} Instance
     * </p>
     *
     * @return {@link WebhookReceiverRenewalService}
     */
    public static WebhookReceiverRenewalService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the given configurations
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.webhookReceiverRenewalTask = new WebhookReceiverRenewalTask(new WebhookReceiverRenewalTaskController(configurations));
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, webhookReceiverRenewalTask);

            LOGGER.info("Webhook Receiver: Webhook receiver renewal service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(webhookReceiverRenewalTask) && webhookReceiverRenewalTask.isRunning()) {
                webhookReceiverRenewalTask.stopTask();
            }

            LOGGER.info("Webhook Receiver: Webhook receiver renewal service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for webhook receiver renewal activity.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class WebhookReceiverRenewalTask extends Task {

        private final EmailWebhookReceiverManagerImpl emailWebHookReceiverManager;
        private final int limit;

        private WebhookReceiverRenewalTask(final TaskController controller) {
            super(controller);
            this.emailWebHookReceiverManager = EmailWebHookReceiverManager.get();

            limit = ConfHolder.getInstance().getProperties(EmailKeys.SYNC_SERVICE.getKey()).getInt(EmailKeys.WEBHOOK_RENEWAL_LIMIT.getKey());
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                LOGGER.info("Webhook Receiver: Webhook receiver renewal scheduler action starts : {}", Instant.now().toEpochMilli());
                final Collection<EmailWebhookReceiver> renewableWebhookReceivers = emailWebHookReceiverManager.getRenewableWebhookReceivers(limit);

                if (Objects.nonNull(renewableWebhookReceivers) && !renewableWebhookReceivers.isEmpty()) {

                    renewableWebhookReceivers.forEach(emailWebhookReceiver -> {
                        try {
                            emailWebhookReceiver.subscribe();
                            emailWebHookReceiverManager.update(emailWebhookReceiver);
                        } catch (Throwable throwable) {
                            LoggerUtil.logError(LOGGER, throwable, "Webhook Receiver: Webhook receiver renewal action failed. message: {}, trace: {}",
                                    throwable.getMessage(), throwable);
                        }
                    });
                }
            } catch (Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Webhook Receiver: Webhook receiver retrieve action failed, message: {}, trace: {}",
                        throwable.getMessage(), throwable);
            }
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
     * A controller which controls webhook renewal  task {@link WebhookReceiverRenewalTask}.
     *
     * @author KowsalyaSp
     * @version 1.0
     */
    private static class WebhookReceiverRenewalTaskController extends TaskController {

        private final long delay;

        public WebhookReceiverRenewalTaskController(final Configurations configurations) {
            super(TASK_SCHEDULER_NAME);
            delay = configurations.getLong(TASK_SCHEDULE_DELAY);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return delay;
        }
    }
}
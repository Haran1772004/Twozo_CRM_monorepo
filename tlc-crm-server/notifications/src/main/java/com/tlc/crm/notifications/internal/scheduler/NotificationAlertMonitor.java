package com.tlc.crm.notifications.internal.scheduler;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.notifications.api.model.AppNotification;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.api.model.alert.ExportAlert;
import com.tlc.crm.notifications.api.model.alert.EntityAssignmentAlert;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SocialChatAlert;
import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;
import com.tlc.crm.notifications.api.model.converter.AppNotificationConverter;
import com.tlc.crm.notifications.service.AppNotificationManager;
import com.tlc.crm.notifications.service.NotificationAlertManager;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.validator.ModelValidator;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.service.provider.ValidatorServiceProvider;
import com.tlc.validator.type.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * Monitors chat alerts.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class NotificationAlertMonitor implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationAlertMonitor.class);
    private static final String THREAD_POOL_NAME = "notification-alert-monitor-pool";
    private static final String TASK_SCHEDULER_NAME = "notification";

    private final SchedulerService schedulerService;

    private Long schedulerId;
    private NotificationAlertMonitorTask notificationAlertMonitorTask;

    private static class Instance {
        private static final NotificationAlertMonitor INSTANCE = new NotificationAlertMonitor();
    }

    private NotificationAlertMonitor() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets {@link NotificationAlertMonitor}.
     * </p>
     *
     * @return {@link NotificationAlertMonitor}.
     */
    public static NotificationAlertMonitor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.notificationAlertMonitorTask = new NotificationAlertMonitorTask(new NotificationAlertMonitorController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, notificationAlertMonitorTask);

            LOGGER.info("Chat alert monitor started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(notificationAlertMonitorTask) && notificationAlertMonitorTask.isRunning()) {
                notificationAlertMonitorTask.stopTask();
            }

            LOGGER.info("Notification alert monitor stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for monitoring alerts.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    private static class NotificationAlertMonitorTask extends Task implements SecureUserTaskProcessor {

        private final AppNotificationManager appNotificationManager;
        private final ModelValidator modelValidator;
        private final NotificationAlertManager notificationAlertManager;
        private final ClusterService clusterService;
        private final AppNotificationConverter appNotificationConverter;

        private NotificationAlertMonitorTask(final TaskController controller) {
            super(controller);
            appNotificationManager = AppNotificationManager.getInstance();
            modelValidator = ValidatorServiceProvider.get().modelValidator();
            notificationAlertManager = NotificationAlertManager.getInstance();
            clusterService = ClusterServiceProvider.get();
            appNotificationConverter = AppNotificationConverter.getInstance();
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
                DistributedLock distributedLock = null; //Todo need to change the logic for distributed lock

                try {
                    distributedLock = clusterService.getDistributedLock("app-monitor");
                    distributedLock.acquire();

                    manageAlerts();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable ignored) {

            }
        }

        /**
         * <p>
         * Manage alerts.
         * </p>
         */
        private void manageAlerts() {
            try {
                final Collection<NotificationAlert> notificationAlerts = notificationAlertManager.getAlerts(
                        Instant.now().toEpochMilli(), NumericalConstant.ONE, NumericalConstant.ONE_HUNDRED);
                final Map<Long, Collection<AppNotification>> appNotificationMap = new HashMap<>();
                final Map<Long, Long> assigneeMap = new HashMap<>();

                for (final NotificationAlert notificationAlert : notificationAlerts) {
                    final Long orgId = notificationAlert.orgId();
                    final NotificationAlert.Category category = notificationAlert.getCategory();

                    switch (category) {
                        case CHAT_ASSIGNED, CHAT_INITIATED -> {
                            final SocialChatAlert chatAlert = (SocialChatAlert) notificationAlert;
                            final Map<Long, Boolean> usersReadMap = chatAlert.getUsersReadMap();

                            for (final Map.Entry<Long, Boolean> userReadEntry : chatAlert.getUsersReadMap().entrySet()) {
                                final Long userId = userReadEntry.getKey();

                                assigneeMap.put(orgId, userId);
                                appNotificationMap.computeIfAbsent(orgId, notification -> new ArrayList<>()).add(
                                        appNotificationConverter.convertChatAlert(usersReadMap.get(userId), userId, chatAlert));
                            }
                        }

                        case ACTIVITY_REMINDER, NEW_ACTIVITY, COLLABORATOR_ADDED -> {
                            final ActivityAlert activityAlert = (ActivityAlert) notificationAlert;
                            final long userId = activityAlert.getAssigneeId();

                            assigneeMap.put(orgId, userId);
                            appNotificationMap.computeIfAbsent(orgId, notification -> new ArrayList<>()).add(
                                    appNotificationConverter.convertActivityAlert(activityAlert.isRead(), userId, activityAlert));
                        }

                        case CONTACT_ASSIGNED, COMPANY_ASSIGNED, DEAL_ASSIGNED, PRODUCT_ASSIGNED -> {
                            final EntityAssignmentAlert entityAssignmentAlert = (EntityAssignmentAlert) notificationAlert;
                            final long userId = entityAssignmentAlert.getAssigneeId();

                            assigneeMap.put(orgId, userId);
                            appNotificationMap.computeIfAbsent(orgId, notification -> new ArrayList<>()).add(
                                    appNotificationConverter.convertEntityAssignmentAlert(entityAssignmentAlert.isRead(), userId, entityAssignmentAlert));
                        }
                        case CONTACT_NOTE_MENTIONED, COMPANY_NOTE_MENTIONED, DEAL_NOTE_MENTIONED, ACTIVITY_PRIVATE_NOTE_MENTIONED,
                             COMPANY_NOTE_COMMENT_MENTIONED, CONTACT_NOTE_COMMENT_MENTIONED, DEAL_NOTE_COMMENT_MENTIONED -> {
                            final SourceMentionAlert sourceMentionAlert = (SourceMentionAlert) notificationAlert;
                            final long userId = sourceMentionAlert.getAssigneeId();

                            assigneeMap.put(orgId, userId);
                            appNotificationMap.computeIfAbsent(orgId, notification -> new ArrayList<>()).add(
                                    appNotificationConverter.convertSourceMentionAlert(sourceMentionAlert.isRead(), userId, sourceMentionAlert));
                        }
                        case EXPORTED -> {
                            final ExportAlert exportAlert = (ExportAlert) notificationAlert;
                            final long userId = exportAlert.getAssigneeId();

                            assigneeMap.put(orgId, userId);
                            appNotificationMap.computeIfAbsent(orgId, notification -> new ArrayList<>()).add(
                                    appNotificationConverter.convertExportAlert(exportAlert.isRead(), userId, exportAlert));
                        }
                    }
                }

                for (final Map.Entry<Long, Collection<AppNotification>> appNotificationMapEntry : appNotificationMap.entrySet()) {
                    final Collection<AppNotification> appNotifications = appNotificationMapEntry.getValue();
                    final Collection<ErrorDescriptor> errors = modelValidator.validate(appNotifications, Group.Create.class);
                    final Long orgId = appNotificationMapEntry.getKey();
                    final Long assigneeId = assigneeMap.get(orgId);

                    if (Objects.nonNull(assigneeId)) {
                        if (errors.isEmpty()) {
                            final Map<Long, List<NotificationAlert>> alertMap = notificationAlerts.stream().
                                    collect(Collectors.groupingBy(NotificationAlert::orgId));

                            secureProcess(orgId, assigneeId, () -> {
                                appNotificationManager.storeNotification(orgId, appNotifications);
                                notificationAlertManager.deleteExpiredAlerts(orgId, alertMap.get(orgId).stream().map(NotificationAlert::id).toList());
                            });
                        } else {
                            LOGGER.error("Notification alert storage failed for Org ID {}, User ID {}", orgId, assigneeId);
                        }
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Notification alert monitor notification conversion failed : {}", e.getMessage());
                e.printStackTrace();
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
     * <p>
     * Monitors {@link NotificationAlertMonitorController}.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    public static class NotificationAlertMonitorController extends TaskController {

        public NotificationAlertMonitorController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.MINUTES.toMillis(2);
        }
    }
}
package com.tlc.crm.notifications.internal.scheduler.call;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.features.action.call.manager.CallLogManager;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.notifications.api.call.manager.CallHistoryManager;
import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.CallSummary;
import com.tlc.crm.notifications.validator.CallDetailsValidator;
import com.tlc.crm.security.context.SecureTaskProcessor;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.notification.model.call.VoiceCall;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.type.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A service that is responsible to trigger and control the completed status {@link CallDetails}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class CallCompletionMonitor implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallCompletionMonitor.class);
    private static final String THREAD_POOL_NAME = "call-completed-status-pool";
    private static final String TASK_SCHEDULER_NAME = "call-completed-status";
    private final SchedulerService schedulerService;
    private Long schedulerId;
    private CallCompletionMonitorTask callCompletionMonitorTask;

    private CallCompletionMonitor() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class Instance {
        private static final CallCompletionMonitor INSTANCE = new CallCompletionMonitor();
    }

    /**
     * <p>
     * Gets the {@link CallCompletionMonitor} Instance
     * </p>
     *
     * @return {@link CallCompletionMonitor}
     */
    public static CallCompletionMonitor getInstance() {
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
            this.callCompletionMonitorTask = new CallCompletionMonitorTask(new CallCompletionMonitorTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, callCompletionMonitorTask);

            LOGGER.info("Call Status: Call completed status service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(callCompletionMonitorTask) && callCompletionMonitorTask.isRunning()) {
                callCompletionMonitorTask.stopTask();
            }

            LOGGER.info("Call Status: Call completed status service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for completed call status
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     */
    private static class CallCompletionMonitorTask extends Task implements SecureTaskProcessor {
        private final VoiceCallService voiceCallService;
        private final CallDetailsValidator callDetailsValidator;
        private final CallHistoryManager callHistoryManager;
        private final CallLogManager callLogManager;
        private final ClusterService clusterService;

        public CallCompletionMonitorTask(final TaskController controller) {
            super(controller);
            callHistoryManager = CallHistoryManager.getInstance();
            voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
            callDetailsValidator = CallDetailsValidator.getInstance();
            callLogManager = CallLogManager.getInstance();
            clusterService = ClusterServiceProvider.get();
        }

        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("call-completed-status");

                    distributedLock.acquire();
                    updateCompletedCalls();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Call Status: Call completed status update failed : {}, Stack {}", e.getMessage(), e);
            }
        }

        /**
         * <p>
         * Updates the completed call details into call summary
         * </p>
         */
        private void updateCompletedCalls() {
            final Collection<CallDetails> completedCallDetails = callHistoryManager.getCompletedCallDetails();

            for (final CallDetails callDetails : completedCallDetails) {
                final VoiceCall voiceCall = voiceCallService.fetchCallDetails(callDetails.getExternalId());
                final CallSummary callSummary = buildCallSummary(voiceCall);

                if (Objects.nonNull(callSummary)) {
                    final Long orgId = callDetails.orgId();
                    final Monetary callCost = callSummary.getCost();

                    if (Objects.nonNull(callCost)) {
                        callCost.setOrgId(orgId);
                    }
                    callSummary.setOrgId(orgId);
                    callDetails.setSummary(callSummary);
                    callDetails.setCallRecordings(voiceCall.getCallRecords());
                    final Collection<String> violations = callDetailsValidator.validate(List.of(callDetails), Group.Update.class)
                            .stream().map(ErrorDescriptor::message).toList();

                    if (violations.isEmpty()) {
                        callHistoryManager.update(callDetails);
                        secureProcess(callDetails.getParticipantUser(), () ->
                                callLogManager.updateStartTime(orgId, callDetails.getCallLogId(), callSummary.getStartTime()));
                    } else {
                        LOGGER.warn("Call History: Violations detected for completed call status: {}", violations);
                    }
                }
            }
        }

        /**
         * Builds a summary of the call including cost, start & end times, and duration.
         *
         * @param voiceCall The voice call object retrieved from the service.
         * @return A populated CallSummary object.
         */
        private CallSummary buildCallSummary(final VoiceCall voiceCall) {
            final BigDecimal price = voiceCall.getPrice();

            if (Objects.isNull(price)) {
                return null;
            }
            final CallSummary callSummary = new CallSummary();
            final String duration = voiceCall.getDuration();

            Currency.getCurrencyByCode(voiceCall.getPriceUnit()).ifPresent(currency -> {
                final Monetary monetary = new Monetary();

                monetary.setValue(price);
                monetary.setCurrency(currency);
                callSummary.setCost(monetary);
            });
            callSummary.setStartTime(new DateTime(voiceCall.getActionTime()));
            callSummary.setEndTime(new DateTime(voiceCall.getCompletedTime()));
            callSummary.setDuration(duration);
            callSummary.setBillingDuration(duration);

            return callSummary;
        }
    }

    /**
     * <p>
     * A controller which controls {@link CallCompletionMonitorTask}
     * </p>
     *
     * @author Kishore J
     * @version 1.0
     */
    private static class CallCompletionMonitorTaskController extends TaskController {

        public CallCompletionMonitorTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(30);
        }
    }
}

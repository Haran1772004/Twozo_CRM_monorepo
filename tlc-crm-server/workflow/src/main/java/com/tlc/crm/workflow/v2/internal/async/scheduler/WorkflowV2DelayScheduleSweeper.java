package com.tlc.crm.workflow.v2.internal.async.scheduler;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.DueSchedule;
import com.tlc.crm.workflow.v2.api.model.ExecutionStatus;
import com.tlc.crm.workflow.v2.api.model.ScheduleType;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;
import com.tlc.crm.workflow.v2.internal.async.WorkflowV2ExecutorConfig;
import com.tlc.crm.workflow.v2.internal.execution.WorkflowV2ExecutionEngine;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Polls due delay schedules and resumes the graph chain directly on the delay resume pool.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class WorkflowV2DelayScheduleSweeper implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2DelayScheduleSweeper.class);

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private DelaySweeperTask task;
    private Executor delayResumeExecutor;
    private long intervalMs;

    private WorkflowV2DelayScheduleSweeper() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static final class Instance {
        private static final WorkflowV2DelayScheduleSweeper INSTANCE = new WorkflowV2DelayScheduleSweeper();
    }

    public static WorkflowV2DelayScheduleSweeper getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.intervalMs = WorkflowV2ExecutorConfig.delaySweeperIntervalMs(configurations);
            final int batchSize = WorkflowV2ExecutorConfig.delaySweeperBatchSize(configurations);
            final String[] poolNameAndSize = WorkflowV2ExecutorConfig.delayResumePool(configurations).split(":");
            this.delayResumeExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0], false);
            this.task = new DelaySweeperTask(new DelaySweeperController("automation-delay-sweeper"), delayResumeExecutor, batchSize);
            this.schedulerId = schedulerService.schedule("automation-delay-sweeper-pool", task);

            LOGGER.info("Workflow V2: Delay schedule sweeper started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Workflow V2: Delay schedule sweeper stopped, Scheduler ID: {}", schedulerId);
        }

        if (Objects.nonNull(delayResumeExecutor)) {
            delayResumeExecutor.shutdown();
        }
    }

    /**
     * Delay sweeper controller for the workflow V2.
     *
     * @author Thalaimalai Pandiyan T
     * @version 2.0
     */
    private final class DelaySweeperController extends TaskController {

        private DelaySweeperController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * Returns the fixed configured interval in milliseconds. The scheduler waits this long after each
         * tick completes before running the next poll.
         *
         * @return delay between sweeper ticks in ms
         */
        @Override
        public long getNextInterval() {
            return intervalMs;
        }
    }

    /**
     * Delay sweeper task for the workflow V2.
     *
     * @author Thalaimalai Pandiyan T
     * @version 2.0
     */
    private static final class DelaySweeperTask extends Task implements SecureUserTaskProcessor {

        private final WorkflowV2ExecutionManager executionManager;
        private final WorkflowV2ExecutionEngine executionEngine;
        private final UserService userService;
        private final Executor delayResumeExecutor;
        private final int batchSize;

        private DelaySweeperTask(final TaskController controller, final Executor delayResumeExecutor, final int batchSize) {
            super(controller);
            this.executionManager = WorkflowV2ExecutionManager.getInstance();
            this.executionEngine = WorkflowV2ExecutionEngine.getInstance();
            this.userService = UserService.getInstance();
            this.delayResumeExecutor = delayResumeExecutor;
            this.batchSize = batchSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                final long totalDue = executionManager.countDueSchedules(ScheduleType.DELAY);
                int processed = 0;

                if (totalDue > 0) {
                    LOGGER.info("Workflow V2: Delay sweeper tick — {} due schedule(s), batchSize {}", totalDue, batchSize);
                }

                while (true) {
                    final Collection<DueSchedule> batch = executionManager.getDueSchedules(ScheduleType.DELAY, batchSize);

                    if (batch.isEmpty()) {
                        break;
                    }

                    for (final DueSchedule dueSchedule : batch) {
                        final Long orgId = dueSchedule.getOrgId();

                        secureProcess(orgId, userService.getAdminUserId(orgId), () -> processDue(dueSchedule));
                        processed++;
                    }
                }

                if (totalDue > 0) {
                    LOGGER.info("Workflow V2: Delay sweeper tick complete — processed {}/{} due schedule(s)", processed, totalDue);
                }
            } catch (Throwable throwable) {
                LOGGER.error("Workflow V2: Delay schedule sweeper failed: {}", throwable.getMessage(), throwable);
            }
        }

        /**
         * Processes the due schedule.
         *
         * @param dueSchedule Represents the due schedule.
         */
        private void processDue(final DueSchedule dueSchedule) {
            final WorkflowV2Execution execution = executionManager.get(dueSchedule.getOrgId(), dueSchedule.getExecutionId());

            if (ExecutionStatus.PENDING != execution.getStatus()) {
                executionManager.claimSchedule(dueSchedule.getOrgId(), dueSchedule.getScheduleId());
                return;
            }

            if (!executionManager.claimSchedule(dueSchedule.getOrgId(), dueSchedule.getScheduleId())) {
                return;
            }

            delayResumeExecutor.execute(() -> {
                try {
                    executionEngine.resumeDelay(dueSchedule.getOrgId(), dueSchedule.getExecutionId(), dueSchedule.getNodeId());
                } catch (Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Workflow V2: Delay resume failed for execution {} node {}: {}",
                            dueSchedule.getExecutionId(), dueSchedule.getNodeId(), throwable.getMessage());
                }
            });
        }
    }
}

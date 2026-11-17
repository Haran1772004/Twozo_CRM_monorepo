package com.tlc.crm.workflow.v2.internal.async.scheduler;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2Execution;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Service responsible for workflow V2 execution history cleanup.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowV2ExecutionHistoryCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2ExecutionHistoryCleaner.class);

    private final SchedulerService schedulerService;

    private WorkflowV2ExecutionHistoryCleanerTask workflowV2ExecutionHistoryCleanerTask;
    private Long schedulerId;

    private WorkflowV2ExecutionHistoryCleaner() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class Instance {
        private static final WorkflowV2ExecutionHistoryCleaner INSTANCE = new WorkflowV2ExecutionHistoryCleaner();
    }

    public static WorkflowV2ExecutionHistoryCleaner getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations configuration properties
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule("automation-history-cleaner-pool",
                    workflowV2ExecutionHistoryCleanerTask = new WorkflowV2ExecutionHistoryCleanerTask(
                            new WorkflowV2ExecutionHistoryCleanerController("automation-v2-history-cleaner")));

            LOGGER.info("Workflow V2: Execution history cleaner service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(workflowV2ExecutionHistoryCleanerTask) && workflowV2ExecutionHistoryCleanerTask.isRunning()) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Workflow V2: Execution history cleaner service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * The {@code WorkflowV2ExecutionHistoryCleanerTask} is responsible for cleaning up the obsolete executions.
     *
     * @author Thalaimalai Pandiyan T
     * @version 1.0.0
     * @see WorkflowV2ExecutionHistoryCleanerTask
     * @since 1.0.0
     */
    private static class WorkflowV2ExecutionHistoryCleanerTask extends Task {

        private final WorkflowV2ExecutionManager workflowV2ExecutionManager;

        private WorkflowV2ExecutionHistoryCleanerTask(final TaskController controller) {
            super(controller);
            this.workflowV2ExecutionManager = WorkflowV2ExecutionManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController controls the trigger task
         */
        @Override
        public void execute(final TaskController taskController) {
            LOGGER.debug("Workflow V2: Execution history log cleanup activity started");

            try {
                clearObsoleteExecutions(new WorkflowV2ExecutionHistoryCleanerPageHandler());
                LOGGER.debug("Workflow V2: Execution history log cleanup activity finished");
            } catch (Throwable cause) {
                LOGGER.error("Workflow V2: Execution history log cleanup activity failed, Error: {}", cause.getMessage());
                LOGGER.debug("Stack: ", cause);
            }
        }

        /**
         * Clears the obsolete executions.
         *
         * @param executionPageHandler The page handler for the executions.
         */
        private void clearObsoleteExecutions(final PageHandler<WorkflowV2Execution> executionPageHandler) {
            final Page<WorkflowV2Execution> currentPage = executionPageHandler.getCurrentPage();

            if (Objects.nonNull(currentPage)) {
                final Collection<WorkflowV2Execution> pageData = currentPage.getData();

                if (Objects.nonNull(pageData) && !pageData.isEmpty()) {
                    workflowV2ExecutionManager.deleteObsoleteExecutions(pageData);
                }
            }

            if (executionPageHandler.hasNextPage()) {
                final Page<WorkflowV2Execution> executionPage = executionPageHandler.goToNextPage();

                if (Objects.nonNull(executionPage.getData())) {
                    clearObsoleteExecutions(executionPageHandler);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * The {@code WorkflowV2ExecutionHistoryCleanerController} is responsible for controlling the execution history cleaner task.
     *
     * @author Thalaimalai Pandiyan T
     * @version 1.0.0
     * @see WorkflowV2ExecutionHistoryCleanerController
     * @since 1.0.0
     */
    private static class WorkflowV2ExecutionHistoryCleanerController extends TaskController {

        private WorkflowV2ExecutionHistoryCleanerController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task in milliseconds
         */
        @Override
        public long getNextInterval() {
            return 900000; // 15 minutes
        }
    }

    /**
     * The {@code WorkflowV2ExecutionHistoryCleanerPageHandler} is responsible for handling the page of obsolete executions.
     *
     * @author Thalaimalai Pandiyan T
     * @version 1.0.0
     * @see WorkflowV2ExecutionHistoryCleanerPageHandler
     * @since 1.0.0
     */
    private static final class WorkflowV2ExecutionHistoryCleanerPageHandler extends PageHandler<WorkflowV2Execution> {

        private static final int DEFAULT_LIMIT = 50;

        private final WorkflowV2ExecutionManager workflowV2ExecutionManager;

        private WorkflowV2ExecutionHistoryCleanerPageHandler() {
            super(DEFAULT_LIMIT);
            this.workflowV2ExecutionManager = WorkflowV2ExecutionManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @return page of obsolete executions
         */
        @Override
        protected Page<WorkflowV2Execution> fetchPageData() {
            return workflowV2ExecutionManager.getObsoleteExecutionHistoryPage(getRecordStartingIndex(), getRecordsPerPage());
        }

        /**
         * {@inheritDoc}
         *
         * @return obsolete execution count
         */
        @Override
        protected long totalRecordsCount() {
            return workflowV2ExecutionManager.getObsoleteExecutionCount();
        }
    }

}

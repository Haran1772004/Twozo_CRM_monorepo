package com.tlc.crm.workflow.internal.async.scheduler;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;

import com.tlc.crm.workflow.api.model.history.WorkflowExecutionHistory;
import com.tlc.crm.workflow.api.manager.history.WorkflowExecutionHistoryManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A service that is responsible for history log cleaner actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowExecutionHistoryCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowExecutionHistoryCleaner.class);

    private final SchedulerService schedulerService;

    private WorkflowExecutionHistoryCleanerTask workflowExecutionHistoryCleanerTask;
    private Long schedulerId;

    private WorkflowExecutionHistoryCleaner() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class Instance {
        private static final WorkflowExecutionHistoryCleaner INSTANCE = new WorkflowExecutionHistoryCleaner();
    }

    public static WorkflowExecutionHistoryCleaner getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule("automation-history-cleaner-pool",
                    workflowExecutionHistoryCleanerTask = new WorkflowExecutionHistoryCleanerTask(new WorkflowExecutionHistoryCleanerController("automation-history-cleaner")));

            LOGGER.info("Workflow: Execution history cleaner service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(workflowExecutionHistoryCleanerTask) && workflowExecutionHistoryCleanerTask.isRunning()) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Workflow: Execution history cleaner service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * A task which is responsible to do the execution history log cleanup activity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class WorkflowExecutionHistoryCleanerTask extends Task {

        private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;

        private WorkflowExecutionHistoryCleanerTask(final TaskController controller) {
            super(controller);
            this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            LOGGER.info("Workflow: Execution history log cleanup activity started");

            try {
                clearObsoleteExecutionHistory(new WorkflowExecutionHistoryCleanerPageHandler());
                LOGGER.info("Workflow: Execution history log cleanup activity finished");
            } catch (Throwable cause) {
                LOGGER.error("Workflow: Execution History log cleanup activity failed, Error: {}", cause.getMessage());
                LOGGER.debug("Stack: ", cause);
            }
        }

        /**
         * Clears the expired execution history log.
         *
         * @param historyPageHandler Refers the {@link PageHandler}.
         */
        private void clearObsoleteExecutionHistory(final PageHandler<WorkflowExecutionHistory> historyPageHandler) {
            final Page<WorkflowExecutionHistory> currentPage = historyPageHandler.getCurrentPage();

            if (Objects.nonNull(currentPage)) {
                final Collection<WorkflowExecutionHistory> pageData = currentPage.getData();

                if (Objects.nonNull(pageData)) {
                    workflowExecutionHistoryManager.delete(pageData.stream().map(WorkflowExecutionHistory::id).collect(Collectors.toUnmodifiableSet()));
                }
            }

            if (historyPageHandler.hasNextPage()) {
                final Page<WorkflowExecutionHistory> historyPage = historyPageHandler.goToNextPage();

                if (Objects.nonNull(historyPage.getData())) {
                    clearObsoleteExecutionHistory(historyPageHandler);
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
     * A controller which controls history log cleaner tasks of workflow.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class WorkflowExecutionHistoryCleanerController extends TaskController {

        private WorkflowExecutionHistoryCleanerController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return 900000; // 15 minutes
        }
    }

    /**
     * Represents the page list for the obsolete history fetch using pagination.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static final class WorkflowExecutionHistoryCleanerPageHandler extends PageHandler<WorkflowExecutionHistory> {

        private static final int DEFAULT_LIMIT = 50;

        private final WorkflowExecutionHistoryManager workflowExecutionHistoryManager;

        private WorkflowExecutionHistoryCleanerPageHandler() {
            super(DEFAULT_LIMIT);
            this.workflowExecutionHistoryManager = WorkflowExecutionHistoryManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @return The {@link Page} of expired history.
         */
        @Override
        protected Page<WorkflowExecutionHistory> fetchPageData() {
            return workflowExecutionHistoryManager.getObsoleteExecutionHistoryPage(getRecordStartingIndex(), getRecordsPerPage());
        }

        /**
         * {@inheritDoc}
         *
         * @return The expired history count.
         */
        @Override
        protected long totalRecordsCount() {
            return workflowExecutionHistoryManager.getObsoleteExecutionHistoryCount();
        }
    }

}

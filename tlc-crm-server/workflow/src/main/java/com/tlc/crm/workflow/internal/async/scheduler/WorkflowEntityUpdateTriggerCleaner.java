package com.tlc.crm.workflow.internal.async.scheduler;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.core.workflow.EntityTriggerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A service that is responsible for entity trigger of update events cleaner actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowEntityUpdateTriggerCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowEntityUpdateTriggerCleaner.class);

    private final SchedulerService schedulerService;

    private EntityUpdateTriggerCleanerTask entityTriggerTask;
    private Long schedulerId;

    private WorkflowEntityUpdateTriggerCleaner() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class InstanceHolder {
        private static final WorkflowEntityUpdateTriggerCleaner INSTANCE = new WorkflowEntityUpdateTriggerCleaner();
    }

    public static WorkflowEntityUpdateTriggerCleaner getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Refers the configurations.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.entityTriggerTask = new EntityUpdateTriggerCleanerTask(new EntityUpdateTriggerCleanerController("automation-entity-trigger-cleaner"));
            this.schedulerId = schedulerService.schedule("automation-entity-trigger-cleaner-pool", entityTriggerTask);

            LOGGER.info("Workflow: Entity update trigger cleaner service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(entityTriggerTask) && entityTriggerTask.isRunning()) {
                entityTriggerTask.stopTask();
                LOGGER.info("Workflow: Entity update trigger cleaner service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * A task which is responsible to do the update event entity trigger cleanup activity.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class EntityUpdateTriggerCleanerTask extends Task {

        private final EntityTriggerManager entityTriggerManager;

        private EntityUpdateTriggerCleanerTask(final TaskController controller) {
            super(controller);
            this.entityTriggerManager = EntityTriggerManager.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            LOGGER.info("Workflow: Entity update trigger cleanup activity started");

            try {
                entityTriggerManager.delete();
                LOGGER.info("Workflow: Entity update trigger cleanup activity finished");
            } catch (Exception cause) {
                LOGGER.error("Workflow: Entity update trigger cleanup activity failed, Error: {}", cause.getMessage());
                LOGGER.debug("Stack: ", cause);
            }
        }
    }

    /**
     * A controller which controls Entity update trigger cleaner tasks of workflow.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class EntityUpdateTriggerCleanerController extends TaskController {

        private EntityUpdateTriggerCleanerController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return 1200000; // 20 minutes
        }
    }

}

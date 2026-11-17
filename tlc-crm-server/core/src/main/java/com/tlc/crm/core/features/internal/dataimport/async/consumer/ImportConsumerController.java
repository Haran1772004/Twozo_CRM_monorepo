package com.tlc.crm.core.features.internal.dataimport.async.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.features.internal.dataimport.async.consumer.ImportTask.ImportTaskController;
import com.tlc.crm.core.features.internal.dataimport.async.consumer.RevertTask.RevertTaskController;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A service that is responsible to trigger and control the data import related consumer actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ImportConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportConsumerController.class);

    private static final String IMPORT_THREAD_POOL_NAME = "data-import-pool";
    private static final String IMPORT_TASK_SCHEDULER_NAME = "data-import-processor";

    private static final String REVERT_THREAD_POOL_NAME = "data-revert-pool";
    private static final String REVERT_TASK_SCHEDULER_NAME = "data-revert-processor";
    private static final String IMPORT_EXECUTOR_NAME = "data-import-executor";

    private final SchedulerService schedulerService;
    private Executor dataImportExecutor;

    private ImportTask importTask;
    private RevertTask revertTask;

    private Long importSchedulerId;
    private Long revertSchedulerId;

    public ImportConsumerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        LOGGER.info("Data import: Consumer creation for generating Import, Revert, and Skip file generators is starting");

        if (Objects.isNull(importSchedulerId) && Objects.nonNull(schedulerService)) {
            this.dataImportExecutor = ExecutorsHandler.createTaskExecutor(10, IMPORT_EXECUTOR_NAME, false);
            this.importSchedulerId = schedulerService.schedule(IMPORT_THREAD_POOL_NAME,
                    importTask = new ImportTask(new ImportTaskController(IMPORT_TASK_SCHEDULER_NAME), dataImportExecutor));

            LOGGER.info("Data import: Import consumer service started, Scheduler ID : {}", importSchedulerId);

            this.revertSchedulerId = schedulerService.schedule(REVERT_THREAD_POOL_NAME,
                    revertTask = new RevertTask(new RevertTaskController(REVERT_TASK_SCHEDULER_NAME)));

            LOGGER.info("Data import: Revert consumer service started, Scheduler ID : {}", revertSchedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        LOGGER.info("Data import: Consumer services for Import, Revert, and Skip file generators is stopping");

        if (Objects.nonNull(importSchedulerId)) {
            schedulerService.removeTask(importSchedulerId);

            if (Objects.nonNull(importTask) && importTask.isRunning()) {
                importTask.stopTask();
            }
            LOGGER.info("Data import: Import consumer service stopped, Scheduler ID : {}", importSchedulerId);
        }

        if (Objects.nonNull(revertSchedulerId)) {
            schedulerService.removeTask(revertSchedulerId);

            if (Objects.nonNull(revertTask) && revertTask.isRunning()) {
                revertTask.stopTask();
            }
            LOGGER.info("Data import: Revert consumer service stopped, Scheduler ID : {}", revertSchedulerId);
        }

        if (Objects.nonNull(dataImportExecutor)) {
            dataImportExecutor.shutdown();
        }
    }
}
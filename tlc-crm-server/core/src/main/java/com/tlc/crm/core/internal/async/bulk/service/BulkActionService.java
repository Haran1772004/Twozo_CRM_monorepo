package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.internal.field.BulkActionFields;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Service is responsible for managing and scheduling asynchronous bulk actions
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class BulkActionService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(BulkActionService.class);

    private final Collection<ScheduledTaskInfo> scheduledTaskInfos;
    private final SchedulerService schedulerService;

    public BulkActionService() {
        scheduledTaskInfos = new HashSet<>();
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Starts the bulk action service by processing the provided configurations
     * </p>
     *
     * @param configurations Represents the {@link Configurations}
     */
    @Override
    public void start(final Configurations configurations) {
        final Collection<String> asyncBulkActionNames = configurations.getValues(BulkActionFields.ASYNC_BULK_ACTION_NAMES);

        for (final String asyncBulkActionName : asyncBulkActionNames) {
            scheduledTaskInfos.addAll(ScheduledTaskInfoFactory.schedule(asyncBulkActionName, configurations, schedulerService));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        for (final ScheduledTaskInfo scheduledTaskInfo : scheduledTaskInfos) {
            final Long scheduledTaskId = scheduledTaskInfo.scheduledTaskId();
            final Task task = scheduledTaskInfo.task();

            if (Objects.nonNull(scheduledTaskId)) {
                schedulerService.removeTask(scheduledTaskId);
            }

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Bulk Action: Async bulk action stopped Scheduler ID: {}", scheduledTaskId);
        }
    }
}

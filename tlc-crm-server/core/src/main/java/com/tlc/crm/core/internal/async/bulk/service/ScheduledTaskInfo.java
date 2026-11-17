package com.tlc.crm.core.internal.async.bulk.service;

import com.tlc.scheduler.task.Task;

/**
 * <p>
 * Represents information about a scheduled task
 * </p>
 *
 * @param scheduledTaskId The unique identifier of scheduled task
 * @param task            The task associated with the scheduler
 * @author Kishore J
 * @version 1.0
 */
record ScheduledTaskInfo(Long scheduledTaskId, Task task) {
}
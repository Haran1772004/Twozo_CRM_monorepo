package com.tlc.commons.internal.executor;

import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.executor.ScheduledExecutor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Provides implementations for the {@link ScheduledExecutor} service, handling tasks immediately or at scheduled intervals.
 *
 * @author sundar
 * @version 1.0
 * @see TaskExecutor
 */
public class ScheduledTaskExecutor extends TaskExecutor implements ScheduledExecutor {

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;

    public ScheduledTaskExecutor(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
        super(scheduledThreadPoolExecutor);
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
    }

    /**
     * {@inheritDoc}
     *
     * @param task     Represents the task to be scheduled
     * @param delay    Represents the delay to be scheduled
     * @param timeUnit Represents the time unit of the delay
     * @return {@link ScheduledFuture} representing the result of the scheduled task
     */
    @Override
    public ScheduledFuture<?> schedule(final RunnableTask task, final long delay, final TimeUnit timeUnit) {
        return scheduledThreadPoolExecutor.schedule(TaskContextWrapper.wrapWithContext(task), delay, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @param task     Represents the task to be scheduled
     * @param delay    Represents the delay to be scheduled
     * @param timeUnit Represents the time unit of the delay
     * @param <R>      Represents the type of the result
     * @return {@link ScheduledFuture} representing the result of the scheduled task
     */
    @Override
    public <R> ScheduledFuture<R> schedule(final CallableTask<R> task, final long delay, final TimeUnit timeUnit) {
        return scheduledThreadPoolExecutor.schedule(TaskContextWrapper.wrapWithContext(task), delay, timeUnit);
    }
}
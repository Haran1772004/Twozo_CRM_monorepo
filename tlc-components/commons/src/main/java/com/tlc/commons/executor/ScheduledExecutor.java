package com.tlc.commons.executor;

import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Provides services to process the tasks by scheduling them
 *
 * @author sundar
 * @version 1.0
 * @see Executor
 */
public interface ScheduledExecutor extends Executor {

    /**
     * Schedules the given {@link RunnableTask} to be executed based on the given delay
     *
     * @param task     Represents the task to be scheduled
     * @param delay    Represents the delay to be scheduled
     * @param timeUnit Represents the time unit of the delay
     * @return {@link ScheduledFuture} representing the result of the scheduled task
     */
    ScheduledFuture<?> schedule(final RunnableTask task, final long delay, final TimeUnit timeUnit);

    /**
     * Schedules the given {@link CallableTask} to be executed based on the given delay
     *
     * @param task     Represents the task to be scheduled
     * @param delay    Represents the delay to be scheduled
     * @param timeUnit Represents the time unit of the delay
     * @param <R>      Represents the type of the result
     * @return {@link ScheduledFuture} representing the result of the scheduled task
     */
    <R> ScheduledFuture<R> schedule(final CallableTask<R> task, final long delay, final TimeUnit timeUnit);
}

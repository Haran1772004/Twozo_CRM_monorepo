package com.tlc.commons.executor.task;

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class acts as a wrapper for the {@link RunnableScheduledFuture}
 *
 * @param <R> Represents the resulting type
 * @author sundar
 * @version 1.0
 * @see RunnableScheduledFuture
 */
public class ScheduledTaskFuture<R> implements RunnableScheduledFuture<R> {

    private final RunnableScheduledFuture<R> future;

    public ScheduledTaskFuture(final RunnableScheduledFuture<R> future) {
        this.future = future;
    }

    /**
     * Check whether the task is scheduled to run repeatedly at regular intervals
     *
     * @return true if the task is scheduled to run repeatedly at regular intervals; otherwise false
     */
    @Override
    public boolean isPeriodic() {
        return future.isPeriodic();
    }

    /**
     * Retrieves the remaining delay before the scheduled future is due to execute.
     *
     * @param timeUnit The time unit in which the remaining delay is expressed.
     * @return The remaining delay before the scheduled future is due to execute.
     */
    @Override
    public long getDelay(final TimeUnit timeUnit) {
        return future.getDelay(timeUnit);
    }

    /**
     * Compares the relative ordering of two tasks based on their scheduled execution times
     *
     * @param delayed Represents the object to get the delay
     * @return A negative integer, zero, or a positive integer if this task is scheduled to execute earlier, at the same time, or later than the specified delayed object
     */
    @Override
    public int compareTo(final Delayed delayed) {
        return future.compareTo(delayed);
    }

    /**
     * Executes the task associated with this future
     */
    @Override
    public void run() {
        future.run();
    }

    /**
     * Cancels the execution of the associated task, if it has not already started, or to interrupt it if it is currently running
     *
     * @param mayInterruptIfRunning If it is set to true, calling cancel(true) will attempt to interrupt the task's thread,
     *                              causing it to throw an InterruptedException if it's in a blocking state (such as waiting for I/O or sleeping)
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Checks whether the associated task is cancelled or not
     *
     * @return true if the task is cancelled otherwise false
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Checks whether the associated task is done or not
     *
     * @return true if the task is done otherwise false
     */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Gets the result of the computation when it becomes available. If the computation has not yet completed, the calling thread will block until the result is available.
     *
     * @return the result of the computation when it becomes available
     * @throws InterruptedException If the calling thread is interrupted while waiting for the result
     * @throws ExecutionException   If the task encounters an exception during its execution
     */
    @Override
    public R get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    /**
     * Gets the result of the computation when it becomes available, but with a timeout. If the computation has not completed within the specified time
     *
     * @param time     Which specifies the maximum time to wait for the result
     * @param timeUnit Which specifies the time unit for the timeout duration
     * @return the result of the computation when it becomes available
     * @throws InterruptedException If the calling thread is interrupted while waiting for the result
     * @throws ExecutionException   If the task encounters an exception during its execution
     * @throws TimeoutException     If the computation has not completed within the specified time
     */
    @Override
    public R get(final long time, final TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(time, timeUnit);
    }
}
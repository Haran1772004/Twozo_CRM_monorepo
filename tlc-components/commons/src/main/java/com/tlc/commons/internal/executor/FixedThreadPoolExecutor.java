package com.tlc.commons.internal.executor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for executing tasks by encapsulating them with context details
 *
 * @author sundar
 * @version 1.0
 */
public class FixedThreadPoolExecutor extends ThreadPoolExecutor {

    public FixedThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    /**
     * Submits the given task and returns the result
     *
     * @param task the task to execute
     * @return the result
     */
    @Override
    public Future<?> submit(final Runnable task) {
        return super.submit(TaskContextWrapper.wrapWithContext(task::run));
    }

    /**
     * Submits the given callable task and returns the result
     *
     * @param task the task to execute
     * @param <T>  the type of the result
     * @return the result
     */
    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return super.submit(TaskContextWrapper.wrapWithContext(task::call));
    }

    /**
     * Executes the given task
     *
     * @param task the task to execute
     */
    @Override
    public void execute(final Runnable task) {
        super.execute(TaskContextWrapper.wrapWithContext(task::run));
    }

    /**
     * Submits the given task and returns the result
     *
     * @param task   the task to execute
     * @param result the result to be returned
     * @param <T>    the type of the result
     * @return the result
     */
    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return super.submit(TaskContextWrapper.wrapWithContext(task::run), result);
    }
}

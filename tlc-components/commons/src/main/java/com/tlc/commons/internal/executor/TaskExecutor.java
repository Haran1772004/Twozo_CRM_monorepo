package com.tlc.commons.internal.executor;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Provides implementations for the {@link Executor} service to handle the runnable tasks
 *
 * @author sundar
 * @version 1.0
 * @see TaskExecutor
 */
public class TaskExecutor implements Executor {

    private final ThreadPoolExecutor threadPoolExecutor;

    public TaskExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    /**
     * {@inheritDoc}
     *
     * @param task Represents the task to execute
     */
    @Override
    public void execute(final RunnableTask task) {
        threadPoolExecutor.execute(TaskContextWrapper.wrapWithContext(task));
    }

    /**
     * {@inheritDoc}
     *
     * @param task Represents the task to submit
     * @return The result of the submitted task
     */
    @Override
    public Future<?> submit(final RunnableTask task) {
        return threadPoolExecutor.submit(TaskContextWrapper.wrapWithContext(task));
    }

    /**
     * {@inheritDoc}
     *
     * @param task   Represents the task to submit
     * @param result The result to be returned
     * @return The result of the submitted task
     * @@param <T>    Represents the type of the result
     */
    @Override
    public <R> Future<R> submit(final RunnableTask task, final R result) {
        return threadPoolExecutor.submit(TaskContextWrapper.wrapWithContext(task), result);
    }

    /**
     * {@inheritDoc}
     *
     * @param task Represents the task to submit
     * @param <R>  Represents the type of the result
     * @return The result of the submitted task
     */
    @Override
    public <R> Future<R> submit(final CallableTask<R> task) {
        return threadPoolExecutor.submit(TaskContextWrapper.wrapWithContext(task));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        threadPoolExecutor.shutdown();
    }
}

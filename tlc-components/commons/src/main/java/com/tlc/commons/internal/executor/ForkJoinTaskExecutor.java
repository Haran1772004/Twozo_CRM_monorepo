package com.tlc.commons.internal.executor;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Provides implementations for the {@link Executor} service, designed to manage runnable tasks concurrently using the {@link ForkJoinPool}
 *
 * @author sundar
 * @version 1.0
 * @see Executor
 */
public class ForkJoinTaskExecutor extends ForkJoinPool implements Executor {

    public ForkJoinTaskExecutor(final int parallelism) {
        super(parallelism);
    }

    /**
     * {@inheritDoc}
     *
     * @param task Represents the task to execute
     */
    @Override
    public void execute(final RunnableTask task) {
        super.execute(TaskContextWrapper.wrapWithContext(task));
    }

    /**
     * {@inheritDoc}
     *
     * @param task Represents the task to submit
     * @return The result of the submitted task
     */
    @Override
    public Future<?> submit(final RunnableTask task) {
        return submit((Runnable) task);
    }

    /**
     * {@inheritDoc}
     *
     * @param task   Represents the task to submit
     * @param result The result to be returned
     * @param <R>    Represents the type of the result
     * @return The result of the submitted task
     */
    @Override
    public <R> Future<R> submit(final RunnableTask task, final R result) {
        return super.submit(TaskContextWrapper.wrapWithContext(task), result);
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
        return super.submit(TaskContextWrapper.wrapWithContext(task));
    }
}
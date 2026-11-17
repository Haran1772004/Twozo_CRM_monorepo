package com.tlc.commons.executor;

import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;

/**
 * Provides services to process the tasks
 *
 * @author sundar
 * @version 1.0
 */
public interface Executor {

    /**
     * Executes the given {@link RunnableTask}
     *
     * @param task Represents the task to execute
     */
    void execute(final RunnableTask task);

    /**
     * Submits the given {@link RunnableTask} and returns the result
     *
     * @param task Represents the task to submit
     * @return The result of the submitted task
     */
    Future<?> submit(final RunnableTask task);

    /**
     * Submits the given {@link RunnableTask} and returns the result
     *
     * @param task   Represents the task to submit
     * @param result The result to be returned
     * @param <R>    Represents the type of the result
     * @return The result of the submitted task
     */
    <R> Future<R> submit(final RunnableTask task, final R result);

    /**
     * Submits the given {@link CallableTask} and returns the result
     *
     * @param task Represents the task to submit
     * @param <R>  Represents the type of the result
     * @return The result of the submitted task
     */
    <R> Future<R> submit(final CallableTask<R> task);

    /**
     * Shutdowns the executor
     */
    void shutdown();
}

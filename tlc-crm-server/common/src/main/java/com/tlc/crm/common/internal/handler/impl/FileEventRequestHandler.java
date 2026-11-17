package com.tlc.crm.common.internal.handler.impl;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.crm.common.handler.EventRequestHandler;

import java.util.concurrent.Future;

/**
 * Provides implementation for {@link EventRequestHandler} to file event requests.
 *
 * @author Sathish
 * @version 1.0
 * @see EventRequestHandler
 */
public class FileEventRequestHandler implements EventRequestHandler {

    private final Executor executor;

    public FileEventRequestHandler(final int maxThreads, final String name) {
        this.executor = ExecutorsHandler.createTaskExecutor(maxThreads, name, false);
    }

    /**
     * Initiates shutdown of the underlying executor used for processing event requests.
     *
     * <p>This method prevents new tasks from being accepted and releases executor
     * resources after pending tasks are completed or terminated based on the executor
     * implementation.
     */
    public void shutdown() {
        this.executor.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @param runnableTask Represents the task to be submitted to process the trigger request.
     * @return {@link Future} that can be used to track the submitted task status.
     */
    @Override
    public Future<?> handle(final RunnableTask runnableTask) {
        return executor.submit(runnableTask);
    }
}
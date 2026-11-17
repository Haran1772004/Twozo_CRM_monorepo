package com.tlc.crm.common.internal.handler.impl;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.crm.common.handler.EventRequestHandler;

import java.util.concurrent.Future;

/**
 * Handler for managing the task of event request processing.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class EventRequestHandlerImpl implements EventRequestHandler {

    private final Executor executor;

    public EventRequestHandlerImpl(final int maxThreads, final String name) {
        this.executor = ExecutorsHandler.createTaskExecutor(maxThreads, name, false);
    }

    /**
     * Shutdown the executor service.
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

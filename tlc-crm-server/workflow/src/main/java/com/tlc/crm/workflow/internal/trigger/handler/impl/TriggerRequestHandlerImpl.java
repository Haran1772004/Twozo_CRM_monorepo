package com.tlc.crm.workflow.internal.trigger.handler.impl;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.executor.ExecutorsHandler;

import com.tlc.crm.workflow.internal.trigger.handler.TriggerRequestHandler;

import java.util.concurrent.Future;

/**
 * Handler for managing the task of trigger request processing.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TriggerRequestHandlerImpl implements TriggerRequestHandler {

    private final Executor executor;

    public TriggerRequestHandlerImpl(final int maxThreads, final String name) {
        this.executor = ExecutorsHandler.createTaskExecutor(maxThreads, name, false);
    }

    /**
     * Shutdown the executor service.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @param runnableTask Represents the task to be submitted to process the trigger request.
     * @return {@link Future} that can be used to track the submitted task status.
     */
    @Override
    public Future<?> execute(final RunnableTask runnableTask) {
        return executor.submit(runnableTask);
    }
}

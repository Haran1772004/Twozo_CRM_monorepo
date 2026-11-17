package com.tlc.crm.common.internal.handler.impl;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.crm.common.handler.EventRequestHandler;

import java.util.concurrent.Future;

/**
 * A handler which handles social event requests
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SocialEventRequestHandler implements EventRequestHandler {

    private final Executor executor;

    public SocialEventRequestHandler(final int maxThreads, final String name) {
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
     * @param runnableTask Represents the task to be submitted to process the request.
     * @return the {@link Future}
     */
    @Override
    public Future<?> handle(final RunnableTask runnableTask) {
        return executor.submit(runnableTask);
    }
}
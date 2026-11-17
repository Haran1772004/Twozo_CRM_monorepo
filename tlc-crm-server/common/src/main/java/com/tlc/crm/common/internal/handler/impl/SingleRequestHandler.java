package com.tlc.crm.common.internal.handler.impl;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.crm.common.internal.handler.RequestHandler;

import java.util.concurrent.Future;

/**
 * Provides implementation for {@link RequestHandler} to handle requests
 *
 * @author sundar
 * @version 1.0
 * @see RequestHandler
 */
public class SingleRequestHandler implements RequestHandler {

    private final Executor executor;

    public SingleRequestHandler(final int maxThreads, final String name) {
        this.executor = ExecutorsHandler.createTaskExecutor(maxThreads, name, false);
    }

    /**
     * Shuts down the executor service
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * {@inheritDoc}
     *
     * @param runnableTask Represents the task to be submitted to process the request
     * @return {@link Future} that can be used to track the submitted task status
     */
    @Override
    public Future<?> handle(final RunnableTask runnableTask) {
        return executor.submit(runnableTask);
    }
}

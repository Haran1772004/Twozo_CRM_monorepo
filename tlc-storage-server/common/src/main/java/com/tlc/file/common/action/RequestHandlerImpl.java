package com.tlc.file.common.action;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;

/**
 * Handler for managing the task of request processing
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RequestHandlerImpl implements RequestHandler {

    private final Executor executor;

    public RequestHandlerImpl(final int maxThreads, final String name) {
        this.executor = ExecutorsHandler.createTaskExecutor(maxThreads, name, false);
    }

    /**
     * Shuts down the executor service.
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

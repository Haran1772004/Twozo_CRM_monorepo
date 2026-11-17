package com.tlc.social.server.handler.internal;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.executor.task.RunnableTask;

import com.tlc.social.server.handler.SocialEventHandler;

import java.util.concurrent.Future;

/**
 * <p>
 * Handles the social runnable events asynchronously
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SocialEventHandlerImpl implements SocialEventHandler {

    private final Executor executor;

    public SocialEventHandlerImpl(final int maxThreads, final String name) {
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
     * @param runnableTask Represents the runnable task to be executed
     * @return the {@link Future}
     */
    @Override
    public Future<?> handle(final RunnableTask runnableTask) {
        return executor.submit(runnableTask);
    }
}
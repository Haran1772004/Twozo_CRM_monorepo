package com.tlc.social.server.handler;

import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;

/**
 * <p>
 * An abstract handler to handle the social runnable events asynchronously
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface SocialEventHandler {

    /**
     * Handles the provided runnable task asynchronously
     *
     * @param runnableTask Represents the runnable task to be executed
     * @return {@link Future} which represents the result of the asynchronous operation
     */
    Future<?> handle(final RunnableTask runnableTask);
}
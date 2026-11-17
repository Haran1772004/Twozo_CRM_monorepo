package com.tlc.crm.common.internal.handler;

import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;

/**
 * Handler for managing the task of request processing
 *
 * @author sundar
 * @version 1.0
 */
public interface RequestHandler {

    /**
     * Handles the task of request processing
     *
     * @param runnableTask Represents the task to be submitted to process the request
     * @return {@link Future} that can be used to track the submitted task status
     */
    Future<?> handle(final RunnableTask runnableTask);
}

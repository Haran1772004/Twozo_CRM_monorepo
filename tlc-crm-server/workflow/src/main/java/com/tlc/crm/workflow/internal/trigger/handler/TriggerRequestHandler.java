package com.tlc.crm.workflow.internal.trigger.handler;

import com.tlc.commons.executor.task.RunnableTask;

import java.util.concurrent.Future;

/**
 * Handler for managing the task of trigger request processing.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TriggerRequestHandler {

    /**
     * Handles the task of trigger request processing.
     *
     * @param runnableTask Represents the task to be submitted to process the trigger request.
     * @return {@link Future} that can be used to track the submitted task status.
     */
    Future<?> execute(final RunnableTask runnableTask);
}

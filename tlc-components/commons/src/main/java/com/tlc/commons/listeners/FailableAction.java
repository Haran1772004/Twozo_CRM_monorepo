package com.tlc.commons.listeners;

/**
 * Defines an action that can handle operation failures.
 *
 * @author Sundar
 * @version 1.0
 */
interface FailableAction {

    /**
     * Invoked when the operation fails.
     *
     * @param cause the cause of the failure
     */
    void onFailure(final Throwable cause);
}

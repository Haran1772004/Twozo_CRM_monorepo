package com.tlc.commons.listeners;

/**
 * Represents a callback mechanism for handling the result of asynchronous operations.
 *
 * @param <R> the type of the response
 * @author Selvakumar
 * @version 1.0
 */
public interface Callback<R> extends FailableAction {

    /**
     * Invoked when the operation completes successfully.
     *
     * @param response the response from the operation
     */
    void onSuccess(R response);
}
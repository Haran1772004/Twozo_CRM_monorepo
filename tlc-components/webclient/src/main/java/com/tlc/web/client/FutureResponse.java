package com.tlc.web.client;

/**
 * Represents an action that may or may not have occurred yet.
 *
 * @param <T> Type of the future result
 * @author Selvakumar
 * @version 1.0
 */
public interface FutureResponse<T> {

    /**
     * Chain the given function with the previous action's result.
     * If the given function throws an exception, the returned future will be failed with this exception.
     * When this future[previous action] fails, the failure will be propagated to the returned future [last action's response] and the given function will not be called.
     *
     * @param responseHandler A handler that has to be called upon the successful completion of the previous action [Current future]
     * @param <R>      Type of the object expected from the future response
     * @return Future response of the given function
     */
    <R> FutureResponse<R> compose(ResponseHandler<T, R> responseHandler);

    /**
     * Chain and execute the given function, if all the chained actions are completed successfully.
     *
     * <b>Note : </b> <i>This is a terminal function and <i/>
     * If multiple success handlers are registered, there is no guarantee that all the success handlers are
     * invoked in the order of its registration.
     *
     * If any error occurred or thrown from the success handlers, it will not be propagated to the error handler
     *
     * @param responseHandler Function that has to be called upon the successful completion of the all actions
     * @return Future of the success response
     */
    FutureResponse<T> onSuccess(ResponseHandler<T, T> responseHandler);

    /**
     * Chain the execute given function, if one of the chained actions is failed with error
     *
     * <b>Note : </b> <i>This is a terminal function and <i/>
     * If multiple success handlers are registered, there is no guarantee that all the success handlers are
     * invoked in the order of its registration.
     *
     * @param responseHandler A function that has to be called upon failure of
     *                 one of the registered events in the method chain
     * @return Future of failure response
     */
    FutureResponse<T> onError(ResponseHandler<Throwable, T> responseHandler);

    /**
     * The result of the operation. This will be null if the operation failed.
     *
     * @return Result of the future
     */
    T result();

    /**
     * The future will be marked as completed if it's either succeeded or failed
     *
     * @return If the action is completed successfully or if the action is failed, it will return true.
     */
    boolean isCompleted();

    /**
     * Checks whether the given future is completed successfully or not
     *
     * @return If the action is completed successfully it will return true.
     */
    boolean succeeded();

    /**
     * Checks whether the given future is failed or not
     *
     * @return If the action is failed, it will return true
     */
    boolean failed();

}

package com.tlc.web.client.internal;

import com.tlc.web.client.FutureResponse;
import com.tlc.web.client.ResponseHandler;
import io.vertx.core.Future;
import io.vertx.core.Promise;

import java.util.function.Function;

/**
 * Represents a result of an action that may or may not have occurred yet.
 *
 * @param <T> Type of future response
 * @author Selvakumar
 * @version 1.0
 */
public final class FutureResponseImpl<T> implements FutureResponse<T> {

    private final Future<T> future;

    public FutureResponseImpl(final Future<T> future) {
        this.future = future;
    }

    /**
     * {@inheritDoc}
     *
     * @param responseHandler A handler that has to be called upon the successful completion of the previous action [Current future]
     * @param <R>      Type of the object expected from the future response
     * @return Future response of the given function
     */
    @Override
    public <R> FutureResponse<R> compose(final ResponseHandler<T, R> responseHandler) {
        final Function<T, Future<R>> wrapperFunction = (v) -> {
            final Promise<R> promise = Promise.promise();

            promise.complete(responseHandler.handle(this.future.result()));

            return promise.future();
        };

        return new FutureResponseImpl<>(this.future.compose(wrapperFunction));
    }

    /**
     * {@inheritDoc}
     *
     * @param responseHandler A handler that has to be called upon the successful completion of the all actions
     * @return Future of the success response
     */
    @Override
    public FutureResponse<T> onSuccess(final ResponseHandler<T, T> responseHandler) {
        final Function<T, Future<T>> wrapper = (v) -> {
            final Promise<T> promise = Promise.promise();

            promise.complete(responseHandler.handle(this.future.result()));

            return promise.future();
        };

        return new FutureResponseImpl<T>(future.onSuccess(wrapper::apply));
    }

    /**
     * {@inheritDoc}
     *
     * @param responseHandler A handler that has to be called upon failure of
     *                 one of the registered events in the method chain
     * @return Future of failure response
     */
    @Override
    public FutureResponse<T> onError(final ResponseHandler<Throwable, T> responseHandler) {
        final Function<Throwable, Future<T>> wrapper = (v) -> {
            final Promise<T> promise = Promise.promise();

            promise.complete(responseHandler.handle(v));

            return promise.future();
        };

        return new FutureResponseImpl<T>(future.onFailure(wrapper::apply));
    }

    /**
     * {@inheritDoc}
     *
     * @return Result of the future
     */
    @Override
    public T result() {
        return future.result();
    }

    /**
     * {@inheritDoc}
     *
     * @return If the action is completed successfully or if the action is failed, it will return true.
     */
    @Override
    public boolean isCompleted() {
        return future.isComplete();
    }

    /**
     * {@inheritDoc}
     *
     * @return If the action is completed successfully it will return true.
     */
    @Override
    public boolean succeeded() {
        return future.succeeded();
    }

    /**
     * {@inheritDoc}
     *
     * @return If the action is failed, it will return true
     */
    @Override
    public boolean failed() {
        return future.failed();
    }

}

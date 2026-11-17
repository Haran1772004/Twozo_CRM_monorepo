package com.tlc.commons.executor.task;

import java.util.concurrent.FutureTask;

/**
 * This class acts as a wrapper for the {@link FutureTask}
 *
 * @param <R> Represents the resulting type
 * @author sundar
 * @version 1.0
 * @see FutureTask
 */
public class RunnableTaskFuture<R> extends FutureTask<R> {

    public RunnableTaskFuture(final Runnable runnable, final R result) {
        super(runnable, result);
    }
}

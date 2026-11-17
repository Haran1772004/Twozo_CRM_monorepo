package com.tlc.commons.executor.task;

import java.util.concurrent.Callable;

/**
 * Represents a task intended for execution that will produce a result
 *
 * @param <R> The type of result produced by the task
 * @author sundar
 * @version 1.0
 */
public interface CallableTask<R> extends Callable<R> {
}

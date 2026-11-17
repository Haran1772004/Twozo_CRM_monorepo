package com.tlc.commons.executor.context;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Immutable snapshot of all context values captured from a thread.
 * Thread-safe — can be passed across thread boundaries.
 *
 * @author kumar
 * @version 1.0
 */
public final class ContextSnapshot {

    private static final ContextSnapshot EMPTY = new ContextSnapshot(Map.of());

    private final Map<Context.Type, Context> values;

    ContextSnapshot(final Map<Context.Type, Context> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    public static ContextSnapshot of(final Map<Context.Type, Context> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY;
        }
        return new ContextSnapshot(values);
    }

    public static ContextSnapshot empty() {
        return EMPTY;
    }

    /**
     * Injects this snapshot into the current thread. Must use try-with-resources.
     */
    public ContextScope attach() {
        return ContextPropagator.getInstance().attach(this);
    }

    @SuppressWarnings("unchecked")
    public <T extends Context> T get(final Context.Type type) {
        return (T) values.get(type);
    }

    /**
     * Wraps a Runnable: inject -> run -> clear ALL.
     */
    public Runnable wrap(final Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (ContextScope ignored = attach()) {
                task.run();
            }
        };
    }

    /**
     * Wraps a Callable: inject -> call -> clear ALL.
     */
    public <R> Callable<R> wrap(final Callable<R> task) {
        Objects.requireNonNull(task, "task must not be null");
        return () -> {
            try (ContextScope ignored = attach()) {
                return task.call();
            }
        };
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    Map<Context.Type, Context> values() {
        return values;
    }
}

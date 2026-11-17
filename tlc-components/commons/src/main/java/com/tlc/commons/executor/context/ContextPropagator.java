package com.tlc.commons.executor.context;

import com.tlc.commons.executor.context.registry.ContextHolderRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Central context propagation engine.
 * Pattern: capture -> inject -> run -> clear ALL.
 *
 * @author kumar
 * @version 1.0
 */
public final class ContextPropagator {

    private static final ContextPropagator INSTANCE = new ContextPropagator();

    private ContextPropagator() {
    }

    public static ContextPropagator getInstance() {
        return INSTANCE;
    }

    /**
     * Captures a snapshot of all registered contexts from the current thread.
     * Each value is defensively copied via {@link Context#copy()}.
     * If a holder's copy() throws, that holder is skipped.
     */
    public ContextSnapshot capture() {
        final ContextHolderRegistry registry = ContextHolderRegistry.getInstance();
        final Map<Context.Type, Context> values = new HashMap<>();

        for (final ContextHolder<?> holder : registry.getContextHolders()) {
            holder.getContext().ifPresent(ctx -> {
                try {
                    values.put(ctx.getType(), ctx.copy());
                } catch (final Exception ignored) {
                    // Skip holders whose copy() fails — don't break entire capture
                }
            });
        }

        if (values.isEmpty()) {
            return ContextSnapshot.empty();
        }
        return new ContextSnapshot(values);
    }

    /**
     * Injects a snapshot into the current thread and returns a scope for cleanup.
     */
    public ContextScope attach(final ContextSnapshot snapshot) {
        if (!snapshot.isEmpty()) {
            final ContextHolderRegistry registry = ContextHolderRegistry.getInstance();

            for (final Map.Entry<Context.Type, Context> entry : snapshot.values().entrySet()) {
                final ContextHolder<?> holder = registry.get(entry.getKey());
                if (holder != null) {
                    holder.setContext(entry.getValue());
                }
            }
        }
        return new ContextScope();
    }

    /**
     * Captures current context and wraps a Runnable with propagation.
     * Returns the original task unchanged if no context exists.
     */
    public Runnable wrapTask(final Runnable task) {
        final ContextSnapshot snapshot = capture();
        if (snapshot.isEmpty()) {
            return task;
        }
        return snapshot.wrap(task);
    }

    /**
     * Captures current context and wraps a Callable with propagation.
     * Returns the original task unchanged if no context exists.
     */
    public <R> Callable<R> wrapTask(final Callable<R> task) {
        final ContextSnapshot snapshot = capture();
        if (snapshot.isEmpty()) {
            return task;
        }
        return snapshot.wrap(task);
    }
}

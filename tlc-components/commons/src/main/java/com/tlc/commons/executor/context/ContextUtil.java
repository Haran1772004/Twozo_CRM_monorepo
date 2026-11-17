package com.tlc.commons.executor.context;

import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for capturing and propagating {@link Context} values across async execution boundaries.
 * <p>
 * Captures all registered {@link ContextHolder} values from the calling thread and provides
 * methods to inject them into async threads with safe cleanup to prevent thread-local leaks.
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public final class ContextUtil {

    private ContextUtil() {
    }

    /**
     * Captures a snapshot of all current context values from the calling thread.
     * <p>
     * Each context is copied via {@link Context#copy()} to ensure thread safety.
     * </p>
     *
     * @return a {@link CapturedContext} holding the snapshot
     */
    public static CapturedContext captureContext() {
        final Map<Context, ContextHolder<?>> contextMap = new HashMap<>();

        for (final ContextHolder<?> contextHolder : ContextHolderRegistry.getInstance().getContextHolders()) {
            contextHolder.getContext().ifPresent(context -> contextMap.put(context.copy(), contextHolder));
        }

        return new CapturedContext(contextMap);
    }

    /**
     * Wraps a {@link Runnable} so that the captured context is injected before execution
     * and safely cleared after execution, regardless of success or failure.
     *
     * @param capturedContext the context snapshot captured via {@link #captureContext()}
     * @param task            the task to wrap
     * @return a new {@link RunnableTask} that propagates context
     */
    public static RunnableTask wrapWithContext(final CapturedContext capturedContext, final RunnableTask task) {
        return () -> runWithContext(capturedContext, task);
    }

    /**
     * Executes the given task with the captured context injected into the current thread.
     * <p>
     * Context values are set before execution and cleared after execution (in a finally block)
     * to prevent thread-local leakage across thread-pool reuse.
     * </p>
     *
     * @param capturedContext the context snapshot captured via {@link #captureContext()}
     * @param task            the task to execute
     */
    public static void runWithContext(final CapturedContext capturedContext, final RunnableTask task) {
        try {
            capturedContext.inject();
            task.run();
        } finally {
            ContextHolderRegistry.getInstance().getContextHolders().forEach(ContextHolder::clear);
        }
    }

    /**
     * Holds a snapshot of context values captured from a thread.
     */
    public static final class CapturedContext {

        private final Map<Context, ContextHolder<?>> contextMap;

        CapturedContext(final Map<Context, ContextHolder<?>> contextMap) {
            this.contextMap = contextMap;
        }

        /**
         * Injects the captured context values into the current thread's context holders.
         */
        void inject() {
            for (final Map.Entry<Context, ContextHolder<?>> entry : contextMap.entrySet()) {
                entry.getValue().setContext(entry.getKey());
            }
        }
    }
}
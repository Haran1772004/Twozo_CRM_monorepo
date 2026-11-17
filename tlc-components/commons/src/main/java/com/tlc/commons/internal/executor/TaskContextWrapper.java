package com.tlc.commons.internal.executor;

import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.executor.task.CallableTask;
import com.tlc.commons.executor.task.RunnableTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This utility wraps the given tasks with context values associated with the current thread
 *
 * @author sundar
 * @version 1.0
 */
interface TaskContextWrapper {

    /**
     * Wraps the given {@link CallableTask} with the contexts associated with the current thread
     *
     * @param task the task to be wrapped
     * @param <R>  Represents the type of the result
     * @return wrapped {@link CallableTask}
     */
    static <R> CallableTask<R> wrapWithContext(final CallableTask<R> task) {
        final Map<Context, ContextHolder<?>> contexts = fetchCurrentContextMap();

        return () -> {

            try {
                loadContextValues(contexts);
                return task.call();
            } finally {
                clearContexts();
            }
        };
    }

    /**
     * Wraps the given {@link RunnableTask} with the contexts associated with the current thread
     *
     * @param task the task to be wrapped
     * @return wrapped {@link RunnableTask}
     */
    static RunnableTask wrapWithContext(final RunnableTask task) {
        final Map<Context, ContextHolder<?>> contexts = fetchCurrentContextMap();

        return () -> {

            try {
                loadContextValues(contexts);
                task.run();
            } finally {
                clearContexts();
            }
        };
    }

    /**
     * Retrieves the context values and their associated holders for the current thread
     *
     * @return a mapping of context holders to their corresponding context values
     */
    private static Map<Context, ContextHolder<?>> fetchCurrentContextMap() {
        final Map<Context, ContextHolder<?>> contexts = new HashMap<>();

        for (final ContextHolder<?> contextHolder : ContextHolderRegistry.getInstance().getContextHolders()) {
            contextHolder.getContext().ifPresent(context -> contexts.put(context.copy(), contextHolder));
        }
        return Collections.unmodifiableMap(contexts);
    }

    /**
     * Loads the given context values into the context holder for the current thread
     *
     * @param contexts the context values to be loaded
     */
    private static void loadContextValues(final Map<Context, ContextHolder<?>> contexts) {

        for (final Map.Entry<Context, ContextHolder<?>> contextEntry : contexts.entrySet()) {
            contextEntry.getValue().setContext(contextEntry.getKey());
        }
    }

    /**
     * Clears the context values for the current thread.
     **/
    private static void clearContexts() {
        ContextHolderRegistry.getInstance().getContextHolders().forEach(ContextHolder::clear);
    }
}
package com.tlc.crm.core.api.analytics.cache.executor;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;

/**
 * <p>
 * Provides a shared {@link Executor} for all analytics async cache operations.
 * All analytics cache implementations should use this shared executor
 * instead of creating their own.
 * </p>
 *
 * @author Karthick V
 * @version 1.0
 */
public final class AnalyticsCacheExecutorProvider {

    private static final String EXECUTOR_NAME = "analytics-cache-worker";
    private static final int EXECUTOR_POOL_SIZE = 4;

    private AnalyticsCacheExecutorProvider() {
    }

    private static class Instance {
        private static final Executor EXECUTOR = ExecutorsHandler.createTaskExecutor(EXECUTOR_POOL_SIZE, EXECUTOR_NAME, true);
    }

    /**
     * <p>
     * Returns the shared analytics cache executor instance.
     * </p>
     *
     * @return The shared {@link Executor}
     */
    public static Executor get() {
        return Instance.EXECUTOR;
    }
}


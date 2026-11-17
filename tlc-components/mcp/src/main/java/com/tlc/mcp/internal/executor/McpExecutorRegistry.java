package com.tlc.mcp.internal.executor;

import com.tlc.commons.util.Configurations;
import com.tlc.mcp.executor.ExecutorStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class McpExecutorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpExecutorRegistry.class);

    private static final int DEFAULT_HARD_TIMEOUT = 300;

    private McpConcurrencyGuard nodeGuard;
    private int hardTimeoutSeconds   = DEFAULT_HARD_TIMEOUT;
    private int maxConcurrentPerNode = 0;

    private ScheduledExecutorService timeoutScheduler;
    private ExecutorService asyncExecutor;

    /**
     * Initialises the registry.
     *
     * @param mcpConfig     MCP configuration block
     * @param workerThreads the mcp-worker pool size; the concurrency guard is capped to
     *                      this value so the semaphore and the pool stay in sync
     */
    public void init(final Configurations mcpConfig, final int workerThreads) {
        hardTimeoutSeconds   = mcpConfig.optInt("execution.hard-timeout-seconds", DEFAULT_HARD_TIMEOUT);
        maxConcurrentPerNode = workerThreads;
        nodeGuard            = new McpConcurrencyGuard(maxConcurrentPerNode);

        timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "mcp-timeout-watcher");
            t.setDaemon(true);
            return t;
        });
        asyncExecutor = Executors.newFixedThreadPool(5);

        LOGGER.info("MCP executor registry initialized: maxConcurrent={} hardTimeout={}s",
                maxConcurrentPerNode, hardTimeoutSeconds);
    }

    public McpConcurrencyGuard nodeGuard() {
        return nodeGuard;
    }

    public int hardTimeoutSeconds() {
        return hardTimeoutSeconds;
    }

    public ExecutorService asyncExecutor() {
        return asyncExecutor;
    }

    public ScheduledFuture<?> scheduleTimeout(Runnable onTimeout, long delayMs) {
        return timeoutScheduler.schedule(onTimeout, delayMs, TimeUnit.MILLISECONDS);
    }

    public Map<String, ExecutorStats> stats() {
        return Map.of("node", new ExecutorStats(
                nodeGuard != null ? nodeGuard.availablePermits() : 0,
                totalActiveCount(),
                0
        ));
    }

    public int totalAvailableSlots() {
        return nodeGuard != null ? nodeGuard.availablePermits() : 0;
    }

    public int totalActiveCount() {
        return maxConcurrentPerNode - (nodeGuard != null ? nodeGuard.availablePermits() : 0);
    }

    public void shutdown() {
        if (timeoutScheduler != null) {
            timeoutScheduler.shutdownNow();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
    }
}

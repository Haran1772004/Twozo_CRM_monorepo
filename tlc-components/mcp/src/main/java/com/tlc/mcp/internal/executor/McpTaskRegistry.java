package com.tlc.mcp.internal.executor;

import com.tlc.mcp.executor.McpTaskHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class McpTaskRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpTaskRegistry.class);
    private static final int DEFAULT_RETENTION_SECONDS = 3600;

    private final ConcurrentHashMap<String, McpTaskStatus> tasks = new ConcurrentHashMap<>();
    private final int retentionSeconds;
    private final ScheduledExecutorService cleaner;

    public McpTaskRegistry(int retentionSeconds) {
        this.retentionSeconds = retentionSeconds > 0 ? retentionSeconds : DEFAULT_RETENTION_SECONDS;
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "mcp-task-cleaner");
            t.setDaemon(true);
            return t;
        });
        final long interval = Math.max(60L, this.retentionSeconds / 4L);
        cleaner.scheduleAtFixedRate(this::evictExpired, interval, interval, TimeUnit.SECONDS);
    }

    public void put(String taskId, McpTaskStatus status) {
        tasks.put(taskId, status);
    }

    public Optional<McpTaskHandle> get(String taskId) {
        final McpTaskStatus status = tasks.get(taskId);
        return Optional.ofNullable(status).map(McpTaskStatus::snapshot);
    }

    public Optional<McpTaskStatus> getStatus(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public void shutdown() {
        cleaner.shutdownNow();
        tasks.clear();
    }

    private void evictExpired() {
        final Instant cutoff = Instant.now().minus(Duration.ofSeconds(retentionSeconds));
        final AtomicInteger removed = new AtomicInteger();
        tasks.entrySet().removeIf(e -> {
            final McpTaskStatus s = e.getValue();
            final Instant completed = s.currentCompletedAt();
            if (s.isTerminal() && completed != null && completed.isBefore(cutoff)) {
                removed.incrementAndGet();
                return true;
            }
            return false;
        });
        if (removed.get() > 0) {
            LOGGER.debug("Evicted {} expired async task(s)", removed.get());
        }
    }
}

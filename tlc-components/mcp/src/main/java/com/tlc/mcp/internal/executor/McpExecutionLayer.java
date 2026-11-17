package com.tlc.mcp.internal.executor;

import com.tlc.commons.executor.context.ContextPropagator;
import com.tlc.commons.executor.context.ContextScope;
import com.tlc.commons.executor.context.ContextSnapshot;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.mcp.executor.McpPipelineChain;
import com.tlc.mcp.executor.McpPipelineLayer;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.registry.ParameterSpec;
import com.tlc.mcp.internal.registry.ToolRegistration;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class McpExecutionLayer implements McpPipelineLayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpExecutionLayer.class);

    private final McpToolVersionRegistry registry;
    private final McpExecutorRegistry executorRegistry;
    private final McpTaskRegistry taskRegistry;

    public McpExecutionLayer(McpToolVersionRegistry registry,
                              McpExecutorRegistry executorRegistry,
                              McpTaskRegistry taskRegistry) {
        this.registry = registry;
        this.executorRegistry = executorRegistry;
        this.taskRegistry = taskRegistry;
    }

    @Override
    public McpToolResult process(McpToolInvocation invocation, McpPipelineChain chain) {
        final Optional<ToolRegistration> regOpt = registry.resolve(invocation.toolName());
        if (regOpt.isEmpty()) {
            return McpToolResult.error("Unknown tool: " + invocation.toolName());
        }
        final ToolRegistration reg = regOpt.get();
        final McpConcurrencyGuard nodeGuard = executorRegistry.nodeGuard();

        if (!nodeGuard.tryAcquire()) {
            return McpToolResult.error("MCP server at capacity — try again shortly");
        }

        final ContextSnapshot snapshot = ContextPropagator.getInstance().capture();
        final long timeoutMs = resolveTimeoutMillis(invocation, reg);

        if (invocation.mode() == ExecutionMode.ASYNC) {
            return executeAsync(reg, invocation, nodeGuard, snapshot, timeoutMs);
        }
        return executeSync(reg, invocation, nodeGuard, snapshot, timeoutMs);
    }

    private McpToolResult executeSync(ToolRegistration reg, McpToolInvocation invocation,
                                       McpConcurrencyGuard nodeGuard,
                                       ContextSnapshot snapshot, long timeoutMs) {
        final AtomicBoolean timedOut = new AtomicBoolean(false);
        final Thread currentThread = Thread.currentThread();
        final ScheduledFuture<?> watchdog = executorRegistry.scheduleTimeout(() -> {
            timedOut.set(true);
            currentThread.interrupt();
        }, timeoutMs);
        try (ContextScope ignored = snapshot.attach()) {
            return invokeExecute(reg, invocation);
        } catch (Exception e) {
            Thread.interrupted();
            if (timedOut.get()) {
                return McpToolResult.error("Tool '" + invocation.toolName()
                        + "' timed out after " + (timeoutMs / 1000) + "s");
            }
            LOGGER.error("Tool '{}' execution failed", invocation.toolName(), e);
            return McpToolResult.error("Internal error: " + e.getMessage());
        } finally {
            watchdog.cancel(false);
            Thread.interrupted();
            nodeGuard.release();
        }
    }

    private McpToolResult executeAsync(ToolRegistration reg, McpToolInvocation invocation,
                                        McpConcurrencyGuard nodeGuard,
                                        ContextSnapshot snapshot, long timeoutMs) {
        final String taskId = UUID.randomUUID().toString();
        final McpTaskStatus status = new McpTaskStatus(taskId, invocation.tenantId());
        taskRegistry.put(taskId, status);

        final AtomicBoolean released = new AtomicBoolean(false);
        final AtomicReference<ScheduledFuture<?>> watcherRef = new AtomicReference<>();

        final Future<?> taskFuture = executorRegistry.asyncExecutor().submit(() -> {
            try (ContextScope ignored = snapshot.attach()) {
                status.transitionToRunning();
                final McpToolResult result = invokeExecute(reg, invocation);
                status.complete(result);
            } catch (Exception e) {
                if (!status.isTerminal()) {
                    LOGGER.error("Async tool '{}' task '{}' failed", invocation.toolName(), taskId, e);
                    status.fail(e.getMessage() != null ? e.getMessage() : "Internal error");
                }
            } finally {
                final ScheduledFuture<?> watcher = watcherRef.get();
                if (watcher != null) {
                    watcher.cancel(false);
                }
                if (released.compareAndSet(false, true)) {
                    nodeGuard.release();
                }
            }
        });

        final ScheduledFuture<?> timeoutWatcher = executorRegistry.scheduleTimeout(() -> {
            if (!status.isTerminal()) {
                taskFuture.cancel(true);
                status.timeout();
                if (released.compareAndSet(false, true)) {
                    nodeGuard.release();
                }
            }
        }, timeoutMs);
        watcherRef.set(timeoutWatcher);

        return McpToolResult.trackingId(taskId);
    }

    private McpToolResult invokeExecute(ToolRegistration reg, McpToolInvocation invocation)
            throws Exception {
        final List<Object> args = new ArrayList<>();
        final Map<String, Object> catchAllMap = new LinkedHashMap<>();
        final Set<String> consumedKeys = new HashSet<>();

        for (ParameterSpec spec : reg.parameters()) {
            if (spec.isMcpToolContext()) {
                args.add(invocation.context());
                continue;
            }
            if (spec.isCatchAll()) {
                args.add(null);
                continue;
            }
            final Object raw = invocation.arguments().get(spec.name());
            consumedKeys.add(spec.name());
            args.add(convertArg(raw, spec.type()));
        }

        for (Map.Entry<String, Object> e : invocation.arguments().entrySet()) {
            if (!consumedKeys.contains(e.getKey())) {
                catchAllMap.put(e.getKey(), e.getValue());
            }
        }
        for (int i = 0; i < reg.parameters().size(); i++) {
            if (reg.parameters().get(i).isCatchAll()) {
                args.set(i, catchAllMap);
            }
        }
        try {
            final Object result = reg.executeMethod().invoke(reg.toolInstance(), args.toArray());

            return (result instanceof McpToolResult mtr) ? mtr
                    : McpToolResult.text(result == null ? "" : result.toString());
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return McpToolResult.text("");
    }

    @SuppressWarnings("unchecked")
    private Object convertArg(Object raw, Class<?> targetType) {
        if (raw == null) return null;
        if (targetType.isInstance(raw)) return raw;
        if (targetType == String.class)                        return String.valueOf(raw);
        if (targetType == int.class || targetType == Integer.class)
            return raw instanceof Number n ? n.intValue()  : Integer.parseInt(String.valueOf(raw));
        if (targetType == long.class || targetType == Long.class)
            return raw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(raw));
        if (targetType == double.class || targetType == Double.class)
            return raw instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(raw));
        if (targetType == boolean.class || targetType == Boolean.class)
            return raw instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(raw));
        if (targetType == JsonObject.class && raw instanceof Map<?, ?> m)
            return Json.object((Map<String, ?>) m);
        if (targetType == JsonArray.class && raw instanceof Collection<?> c)
            return Json.array(c);
        return targetType.cast(raw);
    }

    private long resolveTimeoutMillis(McpToolInvocation invocation, ToolRegistration reg) {
        if (invocation.timeoutMillis() > 0) {
            return invocation.timeoutMillis();
        }
        final int hard = executorRegistry.hardTimeoutSeconds();
        final int soft = reg.timeoutSeconds();
        return (long) ((soft < 0) ? hard : Math.min(soft, hard)) * 1000;
    }
}

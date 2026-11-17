package com.tlc.mcp.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.mcp.McpService;
import com.tlc.mcp.executor.ExecutorStats;
import com.tlc.mcp.executor.McpMonitor;
import com.tlc.mcp.executor.McpTaskHandle;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.executor.TaskState;
import com.tlc.mcp.internal.executor.McpAuthLayer;
import com.tlc.mcp.internal.executor.McpExecutionLayer;
import com.tlc.mcp.internal.executor.McpExecutorRegistry;
import com.tlc.mcp.internal.executor.McpTaskRegistry;
import com.tlc.mcp.internal.executor.McpToolPipeline;
import com.tlc.mcp.internal.executor.McpValidationLayer;
import com.tlc.mcp.internal.registrar.McpPromptRegistrar;
import com.tlc.mcp.internal.registrar.McpResourceRegistrar;
import com.tlc.mcp.internal.registrar.McpToolRegistrar;
import com.tlc.mcp.internal.registry.McpPromptRegistry;
import com.tlc.mcp.internal.registry.McpResourceRegistry;
import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.registry.PromptRegistration;
import com.tlc.mcp.internal.registry.ResourceRegistration;
import com.tlc.mcp.internal.registry.ToolRegistration;
import com.tlc.mcp.internal.schema.McpSchemaGenerator;
import com.tlc.mcp.internal.schema.McpToolValidator;
import com.tlc.mcp.prompt.McpPromptInfo;
import com.tlc.mcp.resource.McpResourceInfo;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolInfo;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class McpServerImpl implements McpService {

    private static final String BUILTIN_TOOL_GET_TASK = "get_task_status";

    private final McpToolVersionRegistry toolRegistry     = new McpToolVersionRegistry();
    private final McpResourceRegistry    resourceRegistry = new McpResourceRegistry();
    private final McpPromptRegistry      promptRegistry   = new McpPromptRegistry();
    private final McpExecutorRegistry    executorRegistry = new McpExecutorRegistry();
    private final McpTaskRegistry        taskRegistry;

    private final McpToolPipeline pipeline;

    private final McpToolRegistrar     toolRegistrar;
    private final McpResourceRegistrar resourceRegistrar;
    private final McpPromptRegistrar   promptRegistrar;

    private final McpMonitor monitor = new McpMonitorImpl();

    public McpServerImpl(final Configurations configurations, final int workerThreads) {
        executorRegistry.init(configurations, workerThreads);

        final int taskRetentionSeconds = configurations.optInt("execution.task-retention-seconds", 3600);
        this.taskRegistry = new McpTaskRegistry(taskRetentionSeconds);

        final McpSchemaGenerator schemaGenerator = new McpSchemaGenerator();
        final McpToolValidator   validator       = new McpToolValidator();

        final McpAuthLayer       authLayer       = new McpAuthLayer(toolRegistry);
        final McpValidationLayer validationLayer = new McpValidationLayer(validator);
        final McpExecutionLayer  executionLayer  =
                new McpExecutionLayer(toolRegistry, executorRegistry, taskRegistry);

        this.pipeline = new McpToolPipeline(authLayer, validationLayer, executionLayer);

        this.toolRegistrar     = new McpToolRegistrar(toolRegistry, schemaGenerator);
        this.resourceRegistrar = new McpResourceRegistrar(resourceRegistry);
        this.promptRegistrar   = new McpPromptRegistrar(promptRegistry);
    }

    @Override
    public void start(final Configurations configurations) {
    }

    @Override
    public void stop() {
        executorRegistry.shutdown();
        taskRegistry.shutdown();
    }

    @Override
    public McpMonitor getMonitor() {
        return monitor;
    }

    @Override
    public McpToolResult invokeTool(String toolName, Map<String, Object> arguments, Object identity) {
        final Map<String, Object> args = arguments != null ? arguments : Map.of();

        if (BUILTIN_TOOL_GET_TASK.equals(toolName)) {
            return handleGetTaskStatus(args);
        }
        final Optional<ToolRegistration> regOpt = toolRegistry.resolve(toolName);
        if (regOpt.isEmpty()) {
            return McpToolResult.error("Unknown tool: " + toolName);
        }
        final ToolRegistration reg = regOpt.get();
        final String requestId = UUID.randomUUID().toString();
        final long timeoutMs = resolveToolTimeout(reg);
        final McpToolContext<Object> context = new McpToolContext<>(toolName, reg.version(), identity);
        final SchemaDefinition schema = reg.toolInstance().resolveSchema(reg.baseSchema(), context);
        final McpToolInvocation invocation = new McpToolInvocation(
                toolName, reg.version(), requestId, args,
                context, schema, reg.mode(), timeoutMs, null);
        return pipeline.execute(invocation);
    }

    @Override
    public McpToolResult readResource(final String uri, final Object identity) {
        final Optional<ResourceRegistration> regOpt = resourceRegistry.get(uri);
        if (regOpt.isEmpty()) {
            return McpToolResult.error("Unknown resource: " + uri);
        }
        final ResourceRegistration reg = regOpt.get();
        final McpToolContext<Object> ctx = new McpToolContext<>(uri, "1", identity);
        if (!reg.instance().isAuthorized(ctx)) {
            return McpToolResult.error("Unauthorized");
        }
        try {
            return reg.instance().read(ctx);
        } catch (Exception e) {
            return McpToolResult.error("Resource read failed: " + e.getMessage());
        }
    }

    @Override
    public McpToolResult invokePrompt(final String name, final Map<String, String> arguments, final Object identity) {
        final Optional<PromptRegistration> regOpt = promptRegistry.resolve(name);
        if (regOpt.isEmpty()) {
            return McpToolResult.error("Unknown prompt: " + name);
        }
        final PromptRegistration reg = regOpt.get();
        final McpToolContext<Object> ctx = new McpToolContext<>(name, reg.version(), identity);
        if (!reg.instance().isAuthorized(ctx)) {
            return McpToolResult.error("Unauthorized");
        }
        final Map<String, String> args = arguments != null ? arguments : Map.of();
        try {
            return reg.instance().get(args, ctx);
        } catch (Exception e) {
            return McpToolResult.error("Prompt invocation failed: " + e.getMessage());
        }
    }

    @Override
    public List<McpToolInfo> tools() {
        return toolsForIdentity(null);
    }

    @Override
    public Optional<McpToolInfo> tool(String name) {
        if (BUILTIN_TOOL_GET_TASK.equals(name)) {
            return Optional.of(builtinGetTaskStatusInfo());
        }
        return toolRegistry.resolve(name).map(this::toToolInfo);
    }

    @Override
    public List<McpResourceInfo> resources() {
        return resourceRegistry.list().stream()
                .map(r -> new McpResourceInfo(r.uri(), r.name(), r.title(), r.mimeType()))
                .toList();
    }

    @Override
    public Optional<McpResourceInfo> resource(String uri) {
        return resourceRegistry.get(uri)
                .map(r -> new McpResourceInfo(r.uri(), r.name(), r.title(), r.mimeType()));
    }

    @Override
    public List<McpPromptInfo> prompts() {
        return promptRegistry.listAll().stream()
                .map(r -> new McpPromptInfo(r.name(), r.title(), r.description(), r.version()))
                .toList();
    }

    @Override
    public Optional<McpPromptInfo> prompt(String name) {
        return promptRegistry.resolve(name)
                .map(r -> new McpPromptInfo(r.name(), r.title(), r.description(), r.version()));
    }

    public List<McpToolInfo> toolsForIdentity(Object identity) {
        final List<McpToolInfo> result = new ArrayList<>();
        result.add(builtinGetTaskStatusInfo());
        for (ToolRegistration reg : toolRegistry.listCanonical()) {
            final McpToolContext<Object> ctx = new McpToolContext<>(reg.name(), reg.version(), identity);
            if (!reg.toolInstance().checkVisible(ctx)) {
                continue;
            }
            final SchemaDefinition schema = reg.toolInstance().resolveSchema(reg.baseSchema(), ctx);
            result.add(new McpToolInfo(reg.name(), reg.version(), reg.title(), reg.description(),
                    reg.mode(), reg.readOnly(), reg.destructive(), reg.idempotent(),
                    reg.deprecated(), schema, schema.toJsonSchema().toString()));
        }
        return Collections.unmodifiableList(result);
    }

    public McpToolRegistrar     getToolRegistrar()     { return toolRegistrar; }
    public McpResourceRegistrar getResourceRegistrar() { return resourceRegistrar; }
    public McpPromptRegistrar   getPromptRegistrar()   { return promptRegistrar; }

    private McpToolResult handleGetTaskStatus(Map<String, Object> arguments) {
        final Object rawTaskId = arguments.get("taskId");
        if (rawTaskId == null) {
            return McpToolResult.error("Missing required argument: taskId");
        }
        final String taskId = String.valueOf(rawTaskId);
        final Optional<McpTaskHandle> handleOpt = taskRegistry.get(taskId);
        if (handleOpt.isEmpty()) {
            return McpToolResult.error("Task not found or expired: " + taskId);
        }
        final McpTaskHandle handle = handleOpt.get();

        final JsonObject response = Json.object();
        response.put("taskId", handle.taskId());
        response.put("status", handle.state().name());
        if (handle.submittedAt() != null) {
            response.put("submittedAt", handle.submittedAt().toString());
        }
        if (handle.completedAt() != null) {
            response.put("completedAt", handle.completedAt().toString());
        }
        if (handle.state() == TaskState.COMPLETED && handle.result() != null) {
            final JsonArray items = Json.array();
            for (McpToolResult.ContentItem item : handle.result().content()) {
                items.put(Json.object().put("type", item.type()).put("data", item.data()));
            }
            response.put("result", items);
        } else if ((handle.state() == TaskState.FAILED || handle.state() == TaskState.TIMEOUT)
                && handle.result() != null && !handle.result().content().isEmpty()) {
            response.put("error", handle.result().content().get(0).data());
        }
        return McpToolResult.json(response);
    }

    private McpToolInfo toToolInfo(ToolRegistration r) {
        final McpToolContext<Object> nullCtx = new McpToolContext<>(r.name(), r.version(), null);
        final SchemaDefinition schema = r.toolInstance().resolveSchema(r.baseSchema(), nullCtx);
        return new McpToolInfo(r.name(), r.version(), r.title(), r.description(),
                r.mode(), r.readOnly(), r.destructive(), r.idempotent(),
                r.deprecated(), schema, r.inputSchemaJson());
    }

    private McpToolInfo builtinGetTaskStatusInfo() {
        return new McpToolInfo(
                BUILTIN_TOOL_GET_TASK, "1", "Get Task Status",
                "Polls the status of an asynchronous tool invocation.",
                ExecutionMode.SYNC, true, false, true, false, null, null);
    }

    private long resolveToolTimeout(ToolRegistration reg) {
        final int hard = executorRegistry.hardTimeoutSeconds();
        final int soft = reg.timeoutSeconds();
        return (long) ((soft < 0) ? hard : Math.min(soft, hard)) * 1000;
    }

    private final class McpMonitorImpl implements McpMonitor {

        @Override
        public int totalAvailableSlots() {
            return executorRegistry.totalAvailableSlots();
        }

        @Override
        public int totalActiveCount() {
            return executorRegistry.totalActiveCount();
        }

        @Override
        public Map<String, ExecutorStats> executorStats() {
            return executorRegistry.stats();
        }

        @Override
        public Map<Object, Integer> tenantUsage() {
            return Map.of();
        }
    }
}

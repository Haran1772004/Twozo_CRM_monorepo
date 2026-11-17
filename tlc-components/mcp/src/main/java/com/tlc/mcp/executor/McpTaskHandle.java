package com.tlc.mcp.executor;

import com.tlc.mcp.tool.McpToolResult;

import java.time.Instant;

public final class McpTaskHandle {

    private final String taskId;
    private final TaskState state;
    private final McpToolResult result;
    private final Instant submittedAt;
    private final Instant completedAt;
    private final Object tenantId;

    public McpTaskHandle(String taskId,
                         TaskState state,
                         McpToolResult result,
                         Instant submittedAt,
                         Instant completedAt,
                         Object tenantId) {
        this.taskId = taskId;
        this.state = state;
        this.result = result;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
        this.tenantId = tenantId;
    }

    public String taskId()       { return taskId; }
    public TaskState state()     { return state; }
    public McpToolResult result(){ return result; }
    public Instant submittedAt() { return submittedAt; }
    public Instant completedAt() { return completedAt; }
    public Object tenantId()     { return tenantId; }

    public boolean isTerminal() {
        return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.TIMEOUT;
    }
}

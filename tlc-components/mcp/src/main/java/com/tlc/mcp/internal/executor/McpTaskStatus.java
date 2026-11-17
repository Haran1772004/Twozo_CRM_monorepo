package com.tlc.mcp.internal.executor;

import com.tlc.mcp.executor.McpTaskHandle;
import com.tlc.mcp.executor.TaskState;
import com.tlc.mcp.tool.McpToolResult;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public final class McpTaskStatus {

    private final String taskId;
    private final Object tenantId;
    private final Instant submittedAt;

    private final AtomicReference<TaskState> state = new AtomicReference<>(TaskState.PENDING);
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile McpToolResult result;

    public McpTaskStatus(String taskId, Object tenantId) {
        this.taskId = taskId;
        this.tenantId = tenantId;
        this.submittedAt = Instant.now();
    }

    public void transitionToRunning() {
        if (state.compareAndSet(TaskState.PENDING, TaskState.RUNNING)) {
            startedAt = Instant.now();
        }
    }

    public void complete(McpToolResult r) {
        result = r;
        completedAt = Instant.now();
        state.set(TaskState.COMPLETED);
    }

    public void fail(String message) {
        result = McpToolResult.error(message != null ? message : "Internal error");
        completedAt = Instant.now();
        state.set(TaskState.FAILED);
    }

    public void timeout() {
        result = McpToolResult.error("Task timed out");
        completedAt = Instant.now();
        state.set(TaskState.TIMEOUT);
    }

    boolean isTerminal() {
        final TaskState s = state.get();
        return s == TaskState.COMPLETED || s == TaskState.FAILED || s == TaskState.TIMEOUT;
    }

    String taskId()              { return taskId; }
    Object tenantId()            { return tenantId; }
    Instant submittedAt()        { return submittedAt; }
    Instant startedAt()          { return startedAt; }
    TaskState currentState()     { return state.get(); }
    McpToolResult currentResult(){ return result; }
    Instant currentCompletedAt() { return completedAt; }

    McpTaskHandle snapshot() {
        return new McpTaskHandle(taskId, state.get(), result, submittedAt, completedAt, tenantId);
    }
}

package com.tlc.mcp.action.internal.executor;

import com.tlc.mcp.executor.McpTaskHandle;
import com.tlc.mcp.executor.TaskState;
import com.tlc.mcp.internal.executor.McpTaskRegistry;
import com.tlc.mcp.internal.executor.McpTaskStatus;
import com.tlc.mcp.tool.McpToolResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class McpTaskRegistryTest {

    private McpTaskRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new McpTaskRegistry(3600);
    }

    @AfterEach
    void tearDown() {
        registry.shutdown();
    }


    @Test
    void put_get_returnsPendingHandle() {
        final McpTaskStatus status = new McpTaskStatus("task-1", null);
        registry.put("task-1", status);

        final Optional<McpTaskHandle> handle = registry.get("task-1");
        assertTrue(handle.isPresent());
        assertEquals("task-1", handle.get().taskId());
        assertEquals(TaskState.PENDING, handle.get().state());
    }

    @Test
    void get_unknownTask_returnsEmpty() {
        assertTrue(registry.get("not-here").isEmpty());
    }


    @Test
    void getStatus_allowsInternalUpdate_visibleViaGet() {
        final McpTaskStatus status = new McpTaskStatus("task-2", null);
        registry.put("task-2", status);

        status.transitionToRunning();
        assertEquals(TaskState.RUNNING, registry.get("task-2").get().state());

        status.complete(McpToolResult.text("done"));
        assertEquals(TaskState.COMPLETED, registry.get("task-2").get().state());
        assertNotNull(registry.get("task-2").get().result());
    }

    @Test
    void task_failsWithErrorResult() {
        final McpTaskStatus status = new McpTaskStatus("task-3", null);
        registry.put("task-3", status);
        status.fail("something broke");

        final McpTaskHandle handle = registry.get("task-3").get();
        assertEquals(TaskState.FAILED, handle.state());
        assertTrue(handle.result().isError());
    }

    @Test
    void task_timeout_reflected() {
        final McpTaskStatus status = new McpTaskStatus("task-4", null);
        registry.put("task-4", status);
        status.timeout();

        assertEquals(TaskState.TIMEOUT, registry.get("task-4").get().state());
    }


    @Test
    void evictExpired_removesTerminalTasksPastRetention() throws Exception {
        final McpTaskRegistry shortRegistry = new McpTaskRegistry(1);
        final McpTaskStatus old = new McpTaskStatus("old-task", null);
        old.complete(McpToolResult.text("done"));
        shortRegistry.put("old-task", old);

        backdateCompletedAt(old, Instant.now().minusSeconds(5));

        invokeEvict(shortRegistry);

        assertTrue(shortRegistry.get("old-task").isEmpty(),
                "Expired terminal task should be evicted");
        shortRegistry.shutdown();
    }

    @Test
    void evictExpired_keepsRecentTerminalTasks() throws Exception {
        final McpTaskRegistry shortRegistry = new McpTaskRegistry(3600);
        final McpTaskStatus recent = new McpTaskStatus("recent-task", null);
        recent.complete(McpToolResult.text("done"));
        shortRegistry.put("recent-task", recent);

        invokeEvict(shortRegistry);

        assertTrue(shortRegistry.get("recent-task").isPresent(),
                "Recently completed task should not be evicted");
        shortRegistry.shutdown();
    }

    @Test
    void evictExpired_doesNotRemovePendingTasks() throws Exception {
        final McpTaskRegistry shortRegistry = new McpTaskRegistry(1);
        final McpTaskStatus pending = new McpTaskStatus("pending-task", null);
        shortRegistry.put("pending-task", pending);

        invokeEvict(shortRegistry);

        assertTrue(shortRegistry.get("pending-task").isPresent(),
                "Non-terminal tasks should never be evicted");
        shortRegistry.shutdown();
    }


    @Test
    void shutdown_clearsAllTasks() {
        final McpTaskStatus s = new McpTaskStatus("x", null);
        registry.put("x", s);
        registry.shutdown();
        assertTrue(registry.get("x").isEmpty());
    }


    private static void invokeEvict(McpTaskRegistry reg) throws Exception {
        final Method m = McpTaskRegistry.class.getDeclaredMethod("evictExpired");
        m.setAccessible(true);
        m.invoke(reg);
    }

    private static void backdateCompletedAt(McpTaskStatus status, Instant instant) throws Exception {
        final Field f = McpTaskStatus.class.getDeclaredField("completedAt");
        f.setAccessible(true);
        f.set(status, instant);
    }
}

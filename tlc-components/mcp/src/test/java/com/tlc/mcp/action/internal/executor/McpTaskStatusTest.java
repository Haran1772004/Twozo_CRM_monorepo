//package com.tlc.mcp.action.internal.executor;
//
//import com.tlc.mcp.executor.McpTaskHandle;
//import com.tlc.mcp.executor.TaskState;
//import com.tlc.mcp.internal.executor.McpTaskStatus;
//import com.tlc.mcp.tool.McpToolResult;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class McpTaskStatusTest {
//
//    @Test
//    void initialState_isPending() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        assertEquals(TaskState.PENDING, status.currentState());
//        assertFalse(status.isTerminal());
//        assertNull(status.currentResult());
//        assertNull(status.currentCompletedAt());
//    }
//
//    @Test
//    void transitionToRunning_setsRunningState() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        status.transitionToRunning();
//        assertEquals(TaskState.RUNNING, status.currentState());
//        assertNotNull(status.startedAt());
//        assertFalse(status.isTerminal());
//    }
//
//    @Test
//    void transitionToRunning_onlyFromPending() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        status.complete(McpToolResult.text("done"));
//        status.transitionToRunning();
//        assertEquals(TaskState.COMPLETED, status.currentState());
//    }
//
//    @Test
//    void complete_setsCompletedStateAndResult() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        final McpToolResult result = McpToolResult.text("done");
//        status.complete(result);
//        assertEquals(TaskState.COMPLETED, status.currentState());
//        assertTrue(status.isTerminal());
//        assertSame(result, status.currentResult());
//        assertNotNull(status.currentCompletedAt());
//    }
//
//    @Test
//    void fail_setsFailedStateWithErrorResult() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        status.fail("something broke");
//        assertEquals(TaskState.FAILED, status.currentState());
//        assertTrue(status.isTerminal());
//        assertTrue(status.currentResult().isError());
//        assertTrue(status.currentResult().content().get(0).data().contains("something broke"));
//    }
//
//    @Test
//    void fail_nullMessage_usesDefault() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        status.fail(null);
//        assertEquals(TaskState.FAILED, status.currentState());
//        assertFalse(status.currentResult().content().get(0).data().isBlank());
//    }
//
//    @Test
//    void timeout_setsTimeoutState() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        status.timeout();
//        assertEquals(TaskState.TIMEOUT, status.currentState());
//        assertTrue(status.isTerminal());
//        assertNotNull(status.currentCompletedAt());
//    }
//
//    @Test
//    void snapshot_capturesCurrentState() {
//        final McpTaskStatus status = new McpTaskStatus("t1", "tenant-42");
//        status.transitionToRunning();
//        final McpTaskHandle snapshot = status.snapshot();
//        assertEquals("t1", snapshot.taskId());
//        assertEquals(TaskState.RUNNING, snapshot.state());
//        assertEquals("tenant-42", snapshot.tenantId());
//        assertNull(snapshot.result());
//        assertNull(snapshot.completedAt());
//    }
//
//    @Test
//    void snapshot_afterComplete_hasResult() {
//        final McpTaskStatus status = new McpTaskStatus("t1", null);
//        final McpToolResult result = McpToolResult.text("answer");
//        status.complete(result);
//        final McpTaskHandle snapshot = status.snapshot();
//        assertEquals(TaskState.COMPLETED, snapshot.state());
//        assertSame(result, snapshot.result());
//        assertNotNull(snapshot.completedAt());
//    }
//}

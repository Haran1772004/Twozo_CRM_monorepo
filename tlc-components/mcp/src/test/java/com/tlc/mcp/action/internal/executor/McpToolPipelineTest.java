package com.tlc.mcp.action.internal.executor;

import com.tlc.mcp.executor.LayerPosition;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.internal.executor.McpAuthLayer;
import com.tlc.mcp.internal.executor.McpExecutionLayer;
import com.tlc.mcp.internal.executor.McpExecutorRegistry;
import com.tlc.mcp.internal.executor.McpTaskRegistry;
import com.tlc.mcp.internal.executor.McpToolPipeline;
import com.tlc.mcp.internal.executor.McpValidationLayer;
import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.schema.McpToolValidator;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class McpToolPipelineTest {

    private McpToolVersionRegistry toolRegistry;
    private McpToolPipeline pipeline;

    @BeforeEach
    void setUp() {
        toolRegistry = new McpToolVersionRegistry();
        final McpAuthLayer authLayer = new McpAuthLayer(toolRegistry);
        final McpValidationLayer validationLayer = new McpValidationLayer(new McpToolValidator());
        final McpExecutionLayer executionLayer = new McpExecutionLayer(
                toolRegistry, new McpExecutorRegistry(), new McpTaskRegistry(60));
        pipeline = new McpToolPipeline(authLayer, validationLayer, executionLayer);
    }

    @Test
    void beforeAuth_layer_shortCircuits_beforeAuth() {
        final List<String> called = new ArrayList<>();
        pipeline.addLayer((inv, chain) -> {
            called.add("before-auth");
            return McpToolResult.text("short-circuited");
        }, LayerPosition.BEFORE_AUTH);

        final McpToolResult result = pipeline.execute(invocation("any-tool"));
        assertEquals("short-circuited", result.content().get(0).data());
        assertEquals(List.of("before-auth"), called);
    }

    @Test
    void multipleBeforeAuth_layers_executedInRegistrationOrder() {
        final List<String> order = new ArrayList<>();
        pipeline.addLayer((inv, chain) -> { order.add("A"); return chain.next(inv); }, LayerPosition.BEFORE_AUTH);
        pipeline.addLayer((inv, chain) -> { order.add("B"); return McpToolResult.text("stop"); }, LayerPosition.BEFORE_AUTH);
        pipeline.addLayer((inv, chain) -> { order.add("C"); return chain.next(inv); }, LayerPosition.BEFORE_AUTH);

        pipeline.execute(invocation("x"));
        assertEquals(List.of("A", "B"), order);
    }

    @Test
    void afterAuth_layer_executedAfterAuthPassThrough() {
        final List<String> order = new ArrayList<>();
        pipeline.addLayer((inv, chain) -> { order.add("before-auth"); return chain.next(inv); }, LayerPosition.BEFORE_AUTH);
        pipeline.addLayer((inv, chain) -> { order.add("after-auth"); return McpToolResult.text("stop"); }, LayerPosition.AFTER_AUTH);

        pipeline.execute(invocation("x"));
        assertEquals(List.of("before-auth", "after-auth"), order);
    }

    @Test
    void afterValidation_layer_calledAfterValidation() {
        final List<String> order = new ArrayList<>();
        pipeline.addLayer((inv, chain) -> {
            final McpToolResult result = chain.next(inv);
            order.add("after-validation-post");
            return result;
        }, LayerPosition.AFTER_VALIDATION);

        pipeline.execute(invocation("unknown-tool"));
        assertTrue(order.contains("after-validation-post"),
                "AFTER_VALIDATION layer post-chain hook should have been called");
    }

    @Test
    void afterValidation_layer_capturesResult() {
        final AtomicReference<McpToolResult> captured = new AtomicReference<>();
        pipeline.addLayer((inv, chain) -> {
            final McpToolResult result = chain.next(inv);
            captured.set(result);
            return result;
        }, LayerPosition.AFTER_VALIDATION);

        pipeline.execute(invocation("unknown-tool"));
        assertNotNull(captured.get());
        assertTrue(captured.get().isError());
    }

    @Test
    void multipleLayersAtSamePosition_executedInOrder() {
        final List<String> order = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            final String label = "L" + i;
            pipeline.addLayer((inv, chain) -> { order.add(label); return chain.next(inv); }, LayerPosition.BEFORE_AUTH);
        }
        pipeline.addLayer((inv, chain) -> McpToolResult.text("stop"), LayerPosition.BEFORE_AUTH);
        pipeline.execute(invocation("x"));
        assertEquals(List.of("L1", "L2", "L3"), order);
    }

    @Test
    void layerCanModifyInvocation_andPassDownstream() {
        final AtomicReference<String> seenTool = new AtomicReference<>();
        pipeline.addLayer((inv, chain) -> {
            final McpToolInvocation modified = new McpToolInvocation(
                    "modified-tool", inv.toolVersion(), inv.requestId(),
                    inv.arguments(), inv.context(), inv.schema(),
                    inv.mode(), inv.timeoutMillis(), inv.tenantId()
            );
            return chain.next(modified);
        }, LayerPosition.BEFORE_AUTH);
        pipeline.addLayer((inv, chain) -> {
            seenTool.set(inv.toolName());
            return McpToolResult.text("captured");
        }, LayerPosition.BEFORE_AUTH);

        pipeline.execute(invocation("original-tool"));
        assertEquals("modified-tool", seenTool.get());
    }


    private static McpToolInvocation invocation(String toolName) {
        return new McpToolInvocation(
                toolName, "1", "req-1", Map.of(),
                new McpToolContext<>(toolName, "1", null),
                null, ExecutionMode.SYNC, 5000L, null
        );
    }
}

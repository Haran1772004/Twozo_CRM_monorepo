package com.tlc.mcp.internal.executor;

import com.tlc.mcp.executor.McpPipelineChain;
import com.tlc.mcp.executor.McpPipelineLayer;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.internal.registry.McpToolVersionRegistry;
import com.tlc.mcp.internal.registry.ToolRegistration;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpToolResult;

import java.util.Optional;

public final class McpAuthLayer implements McpPipelineLayer {

    private final McpToolVersionRegistry registry;

    public McpAuthLayer(McpToolVersionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public McpToolResult process(McpToolInvocation invocation, McpPipelineChain chain) {
        final Optional<ToolRegistration> regOpt = registry.resolve(invocation.toolName());
        if (regOpt.isPresent()) {
            final Object instance = regOpt.get().toolInstance();
            if (instance instanceof AbstractMcpTool<?> tool) {
                if (!tool.checkAuthorized(invocation.context())) {
                    return McpToolResult.error("Unauthorized");
                }
            }
        }
        return chain.next(invocation);
    }
}

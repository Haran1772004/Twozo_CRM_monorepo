package com.tlc.mcp.executor;

import com.tlc.mcp.tool.McpToolResult;

@FunctionalInterface
public interface McpPipelineChain {

    McpToolResult next(McpToolInvocation invocation);
}

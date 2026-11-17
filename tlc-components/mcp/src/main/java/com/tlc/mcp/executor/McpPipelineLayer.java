package com.tlc.mcp.executor;

import com.tlc.mcp.tool.McpToolResult;

public interface McpPipelineLayer {

    McpToolResult process(McpToolInvocation invocation, McpPipelineChain chain);
}

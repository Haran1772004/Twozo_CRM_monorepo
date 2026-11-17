package com.tlc.mcp.resource;

import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;

public interface McpResource {

    McpToolResult read(McpToolContext<?> ctx);

    default boolean isAuthorized(McpToolContext<?> ctx) {
        return true;
    }
}

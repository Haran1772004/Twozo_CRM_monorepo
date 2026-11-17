package com.tlc.mcp.prompt;

import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;

import java.util.Map;

public interface McpPrompt {

    McpToolResult get(Map<String, String> arguments, McpToolContext<?> ctx);

    default boolean isAuthorized(McpToolContext<?> ctx) {
        return true;
    }
}

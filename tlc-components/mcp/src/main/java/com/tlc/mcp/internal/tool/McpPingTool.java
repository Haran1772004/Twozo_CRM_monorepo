package com.tlc.mcp.internal.tool;

import com.tlc.commons.json.Json;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpTool;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.ToolParam;

import java.time.Instant;

@McpTool(
        name        = "mcp_ping",
        description = "Built-in smoke-test tool. Echoes the message back with server info " +
                      "and the resolved identity — use this to verify MCP connectivity and " +
                      "identity wiring end-to-end.",
        readOnly    = true
)
public final class McpPingTool extends AbstractMcpTool<Object> {

    public McpToolResult execute(
            @ToolParam(description = "Message to echo back", required = false, defaultValue = "ping")
            String message,
            McpToolContext<Object> ctx) {

        return McpToolResult.json(Json.object()
                .put("echo",      message != null ? message : "ping")
                .put("server",    "tlc-mcp-server")
                .put("timestamp", Instant.now().toString())
                .put("identity",  ctx.identity() != null ? ctx.identity().toString() : "anonymous"));
    }
}

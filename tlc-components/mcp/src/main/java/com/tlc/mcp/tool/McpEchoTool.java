package com.tlc.mcp.tool;

/**
 * Built-in diagnostic tool that echoes back the provided message.
 *
 * <p>Shipped with the MCP module and auto-discovered at startup via classpath scan.
 * Useful for verifying that the MCP server is running and tool dispatch is working
 * end-to-end without needing an application-specific tool.</p>
 *
 * <p>Example call:</p>
 * <pre>{@code
 * POST /rest/api/mcp
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "method": "tools/call",
 *   "params": { "name": "mcp_echo", "arguments": { "message": "hello" } }
 * }
 * }</pre>
 *
 */
@McpTool(
        name = "mcp_echo",
        title = "MCP Echo",
        description = "Echoes the provided message back. Use this to verify the MCP server is reachable and tool dispatch is working.",
        readOnly = true,
        idempotent = true
)
public class McpEchoTool extends AbstractMcpTool<Object> {

    public McpToolResult execute(
            @ToolParam(description = "The message to echo back", minLength = 1, maxLength = 1000) final String message) {
        return McpToolResult.text(message);
    }
}

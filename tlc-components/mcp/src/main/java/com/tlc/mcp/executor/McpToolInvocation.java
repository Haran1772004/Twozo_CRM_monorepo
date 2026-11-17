package com.tlc.mcp.executor;

import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.McpToolContext;
import com.tlc.mcp.tool.SchemaDefinition;

import java.util.Map;

public record McpToolInvocation(
        String toolName,
        String toolVersion,
        String requestId,
        Map<String, Object> arguments,
        McpToolContext<?> context,
        SchemaDefinition schema,
        ExecutionMode mode,
        long timeoutMillis,
        Object tenantId
) {}

package com.tlc.mcp.tool;

public record McpToolInfo(
        String name,
        String version,
        String title,
        String description,
        ExecutionMode mode,
        boolean readOnly,
        boolean destructive,
        boolean idempotent,
        boolean deprecated,
        SchemaDefinition schema,
        String inputSchemaJson
) {}

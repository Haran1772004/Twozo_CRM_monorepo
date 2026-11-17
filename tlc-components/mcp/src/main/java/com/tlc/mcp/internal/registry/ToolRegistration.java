package com.tlc.mcp.internal.registry;

import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.ExecutionMode;
import com.tlc.mcp.tool.SchemaDefinition;

import java.lang.reflect.Method;
import java.util.List;

public record ToolRegistration(
        String name,
        String version,
        String title,
        String description,
        boolean readOnly,
        boolean destructive,
        boolean idempotent,
        boolean deprecated,
        String executor,
        int timeoutSeconds,
        ExecutionMode mode,
        AbstractMcpTool<?> toolInstance,
        Method executeMethod,
        List<ParameterSpec> parameters,
        SchemaDefinition baseSchema,
        String inputSchemaJson
) {}

package com.tlc.mcp.internal.executor;

import com.tlc.mcp.executor.McpPipelineChain;
import com.tlc.mcp.executor.McpPipelineLayer;
import com.tlc.mcp.executor.McpToolInvocation;
import com.tlc.mcp.internal.schema.McpToolValidator;
import com.tlc.mcp.internal.schema.McpValidationException;
import com.tlc.mcp.tool.McpToolResult;
import com.tlc.mcp.tool.SchemaDefinition;

public final class McpValidationLayer implements McpPipelineLayer {

    private final McpToolValidator validator;

    public McpValidationLayer(McpToolValidator validator) {
        this.validator = validator;
    }

    @Override
    public McpToolResult process(McpToolInvocation invocation, McpPipelineChain chain) {
        final SchemaDefinition schema = invocation.schema();
        if (schema != null) {
            try {
                validator.validate(invocation.arguments(), schema);
            } catch (McpValidationException e) {
                return McpToolResult.error("Validation failed: " + String.join("; ", e.errors()));
            }
        }
        return chain.next(invocation);
    }
}

package com.tlc.mcp.internal.schema;

import java.util.List;

public final class McpValidationException extends RuntimeException {

    private final List<String> errors;

    public McpValidationException(List<String> errors) {
        super("Validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}

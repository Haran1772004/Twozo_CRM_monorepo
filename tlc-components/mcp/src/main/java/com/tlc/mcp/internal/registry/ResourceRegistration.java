package com.tlc.mcp.internal.registry;

import com.tlc.mcp.resource.McpResource;

public record ResourceRegistration(
        String uri,
        String name,
        String title,
        String mimeType,
        McpResource instance
) {}

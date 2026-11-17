package com.tlc.mcp.resource;

public record McpResourceInfo(
        String uri,
        String name,
        String title,
        String mimeType
) {}

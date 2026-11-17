package com.tlc.mcp.internal.registry;

import com.tlc.mcp.prompt.McpPrompt;

public record PromptRegistration(
        String name,
        String title,
        String description,
        String version,
        McpPrompt instance
) {}

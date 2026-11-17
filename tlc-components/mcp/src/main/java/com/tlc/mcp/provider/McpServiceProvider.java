package com.tlc.mcp.provider;

import com.tlc.mcp.McpService;
import com.tlc.mcp.internal.registry.McpServiceRegistry;

public final class McpServiceProvider {

    private McpServiceProvider() {}

    public static McpService get() {
        return McpServiceRegistry.getInstance().get();
    }
}

package com.tlc.mcp.internal.registry;

public record ParameterSpec(
        String name,
        Class<?> type,
        boolean isMcpToolContext,
        boolean isCatchAll
) {}

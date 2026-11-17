package com.tlc.mcp.executor;

import java.util.Map;

public interface McpMonitor {

    int totalAvailableSlots();

    int totalActiveCount();

    Map<String, ExecutorStats> executorStats();

    Map<Object, Integer> tenantUsage();
}

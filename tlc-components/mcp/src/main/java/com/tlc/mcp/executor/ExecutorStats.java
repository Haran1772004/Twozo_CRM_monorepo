package com.tlc.mcp.executor;

public record ExecutorStats(int availableSlots, int activeCount, int queueDepth) {}

package com.tlc.mcp.internal.registry;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.mcp.McpService;

import java.util.concurrent.atomic.AtomicReference;

public final class McpServiceRegistry implements ServiceRegistry<McpService> {

    private static final McpServiceRegistry INSTANCE = new McpServiceRegistry();

    private final AtomicReference<McpService> ref = new AtomicReference<>();

    private McpServiceRegistry() {}

    public static McpServiceRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(McpService service) {
        if (!ref.compareAndSet(null, service)) {
            throw new IllegalStateException("McpService already registered");
        }
    }

    @Override
    public McpService get() {
        final McpService service = ref.get();
        if (service == null) {
            throw new IllegalStateException("McpService not yet registered");
        }
        return service;
    }

    @Override
    public McpService unregister() {
        return ref.getAndSet(null);
    }
}

package com.tlc.mcp.internal.registrar;

import com.tlc.mcp.internal.registry.McpResourceRegistry;
import com.tlc.mcp.internal.registry.ResourceRegistration;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.resource.McpResourceDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McpResourceRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpResourceRegistrar.class);

    private final McpResourceRegistry registry;

    public McpResourceRegistrar(McpResourceRegistry registry) {
        this.registry = registry;
    }

    public void register(String bundleName, Class<?> resourceClass) {
        final McpResourceDef def = resourceClass.getAnnotation(McpResourceDef.class);
        if (def == null || !McpResource.class.isAssignableFrom(resourceClass)) {
            return;
        }
        try {
            final McpResource instance = (McpResource) resourceClass.getConstructor().newInstance();
            final String title = def.title().isEmpty() ? def.name() : def.title();
            registry.register(bundleName, new ResourceRegistration(
                    def.uri(), def.name(), title, def.mimeType(), instance));
            LOGGER.info("Registered MCP resource '{}' from bundle '{}'", def.uri(), bundleName);
        } catch (Exception e) {
            LOGGER.error("Failed to register resource class '{}' from bundle '{}'",
                    resourceClass.getName(), bundleName, e);
        }
    }

    public void unregisterBundle(String bundleName) {
        registry.unregisterBundle(bundleName);
    }
}

package com.tlc.mcp.internal.registrar;

import com.tlc.mcp.internal.registry.McpPromptRegistry;
import com.tlc.mcp.internal.registry.PromptRegistration;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.prompt.McpPromptDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McpPromptRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpPromptRegistrar.class);

    private final McpPromptRegistry registry;

    public McpPromptRegistrar(McpPromptRegistry registry) {
        this.registry = registry;
    }

    public void register(String bundleName, Class<?> promptClass) {
        final McpPromptDef def = promptClass.getAnnotation(McpPromptDef.class);
        if (def == null || !McpPrompt.class.isAssignableFrom(promptClass)) {
            return;
        }
        try {
            final McpPrompt instance = (McpPrompt) promptClass.getConstructor().newInstance();
            final String title = def.title().isEmpty() ? def.name() : def.title();
            registry.register(bundleName, new PromptRegistration(
                    def.name(), title, def.description(), def.version(), instance));
            LOGGER.info("Registered MCP prompt '{}' v{} from bundle '{}'",
                    def.name(), def.version(), bundleName);
        } catch (Exception e) {
            LOGGER.error("Failed to register prompt class '{}' from bundle '{}'",
                    promptClass.getName(), bundleName, e);
        }
    }

    public void unregisterBundle(String bundleName) {
        registry.unregisterBundle(bundleName);
    }
}

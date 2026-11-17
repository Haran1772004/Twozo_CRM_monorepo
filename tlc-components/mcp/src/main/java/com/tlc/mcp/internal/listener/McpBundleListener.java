package com.tlc.mcp.internal.listener;

import com.tlc.commons.service.listener.BundleListener;
import com.tlc.mcp.internal.registrar.McpPromptRegistrar;
import com.tlc.mcp.internal.registrar.McpResourceRegistrar;
import com.tlc.mcp.internal.registrar.McpToolRegistrar;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.prompt.McpPromptDef;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.resource.McpResourceDef;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpTool;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class McpBundleListener extends BundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpBundleListener.class);

    private static final String TOOL_PATH     = "/mcp/tool/";
    private static final String RESOURCE_PATH = "/mcp/resource/";
    private static final String PROMPT_PATH   = "/mcp/prompt/";

    private final McpToolRegistrar     toolRegistrar;
    private final McpResourceRegistrar resourceRegistrar;
    private final McpPromptRegistrar   promptRegistrar;

    public McpBundleListener(McpToolRegistrar toolRegistrar,
                              McpResourceRegistrar resourceRegistrar,
                              McpPromptRegistrar promptRegistrar) {
        this.toolRegistrar     = toolRegistrar;
        this.resourceRegistrar = resourceRegistrar;
        this.promptRegistrar   = promptRegistrar;
    }

    @Override
    public void bundleStarted(Bundle bundle) {
        final String name = bundle.getSymbolicName();
        int tools = 0, resources = 0, prompts = 0;

        for (Class<?> cls : getClasses(TOOL_PATH, bundle)) {
            if (AbstractMcpTool.class.isAssignableFrom(cls) && cls.isAnnotationPresent(McpTool.class)) {
                toolRegistrar.register(name, cls);
                tools++;
            }
        }
        for (Class<?> cls : getClasses(RESOURCE_PATH, bundle)) {
            if (McpResource.class.isAssignableFrom(cls) && cls.isAnnotationPresent(McpResourceDef.class)) {
                resourceRegistrar.register(name, cls);
                resources++;
            }
        }
        for (Class<?> cls : getClasses(PROMPT_PATH, bundle)) {
            if (McpPrompt.class.isAssignableFrom(cls) && cls.isAnnotationPresent(McpPromptDef.class)) {
                promptRegistrar.register(name, cls);
                prompts++;
            }
        }

        if (tools + resources + prompts > 0) {
            LOGGER.info("Bundle '{}': registered {} tool(s), {} resource(s), {} prompt(s)",
                    name, tools, resources, prompts);
        }
    }

    @Override
    public void bundleStopped(Bundle bundle) {
        final String name = bundle.getSymbolicName();
        toolRegistrar.unregisterBundle(name);
        resourceRegistrar.unregisterBundle(name);
        promptRegistrar.unregisterBundle(name);
        LOGGER.info("Unregistered MCP components for bundle '{}'", name);
    }

    @Override
    public void bundleStopping(Bundle bundle) {}
}

package com.tlc.components.starter.initializer.components;

import com.tlc.commons.util.Configurations;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.mcp.action.McpIdentityExtractor;
import com.tlc.mcp.action.McpIdentityProvider;
import com.tlc.mcp.action.rest.McpHttpTransport;
import com.tlc.mcp.internal.McpServerImpl;
import com.tlc.mcp.internal.registry.McpServiceRegistry;
import com.tlc.mcp.prompt.McpPrompt;
import com.tlc.mcp.prompt.McpPromptDef;
import com.tlc.mcp.resource.McpResource;
import com.tlc.mcp.resource.McpResourceDef;
import com.tlc.mcp.tool.AbstractMcpTool;
import com.tlc.mcp.tool.McpTool;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.Executors;

@Component
final class McpInitializer extends AbstractModuleInitializer {

    private static final String MODULE = "mcp";

    private McpServerImpl server;

    private McpInitializer() {}

    @Override
    public void initialize() {
        if (!isServiceEnabled(MODULE)) {
            return;
        }
        final Configurations configurations = getProperties(MODULE);


        final int workerThreads = configurations.optInt("worker.threads", 20);
        McpHttpTransport.setWorkerPool(Executors.newFixedThreadPool(workerThreads, r -> {
            final Thread t = new Thread(r, "mcp-worker");
            t.setDaemon(true);
            return t;
        }));
        server = new McpServerImpl(configurations, workerThreads);
        server.start(configurations);

        McpServiceRegistry.getInstance().register(server);
        LOGGER.info("MCP service initialized");
    }

    @Override
    public void postInitialize() {
        if (!isServiceEnabled(MODULE) || server == null) {
            return;
        }

        int tools = 0, resources = 0, prompts = 0;

        final Collection<Class<?>> toolClasses =
                classScanner.findClassesWithAnnotation("com.tlc", McpTool.class);
        for (Class<?> cls : toolClasses) {
            if (AbstractMcpTool.class.isAssignableFrom(cls) && cls.getPackageName().contains(".tool")) {
                server.getToolRegistrar().register(MODULE, cls);
                tools++;
            }
        }

        final Collection<Class<?>> resourceClasses =
                classScanner.findClassesWithAnnotation("com.tlc", McpResourceDef.class);
        for (Class<?> cls : resourceClasses) {
            if (McpResource.class.isAssignableFrom(cls) && cls.getPackageName().contains(".resource")) {
                server.getResourceRegistrar().register(MODULE, cls);
                resources++;
            }
        }

        final Collection<Class<?>> promptClasses =
                classScanner.findClassesWithAnnotation("com.tlc", McpPromptDef.class);
        for (Class<?> cls : promptClasses) {
            if (McpPrompt.class.isAssignableFrom(cls) && cls.getPackageName().contains(".prompt")) {
                server.getPromptRegistrar().register(MODULE, cls);
                prompts++;
            }
        }

        LOGGER.info("MCP post-init: registered {} tool(s), {} resource(s), {} prompt(s)",
                tools, resources, prompts);

        wireIdentityExtractor();
        McpHttpTransport.setToolsResolver(server::toolsForIdentity);
    }

    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    @PreDestroy
    public void destroy() {
        McpHttpTransport.shutdownWorkerPool();
    }

    private void wireIdentityExtractor() {
        final Collection<Class<?>> providers =
                classScanner.findClassesWithAnnotation("com.tlc", McpIdentityProvider.class);
        if (providers.isEmpty()) {
            LOGGER.info("No @McpIdentityProvider found — MCP identity will be null for all calls");
            return;
        }
        if (providers.size() > 1) {
            LOGGER.warn("Multiple @McpIdentityProvider classes found ({}); using the first one",
                    providers.size());
        }
        final Class<?> providerClass = providers.iterator().next();
        try {
            final McpIdentityExtractor extractor =
                    (McpIdentityExtractor) providerClass.getDeclaredConstructor().newInstance();
            McpHttpTransport.setIdentityExtractor(extractor);
            LOGGER.info("MCP identity extractor wired: {}", providerClass.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to instantiate @McpIdentityProvider {}: {}",
                    providerClass.getName(), e.getMessage(), e);
        }
    }
}

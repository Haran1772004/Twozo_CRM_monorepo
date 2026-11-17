package com.tlc.mcp;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.mcp.action.rest.McpHttpTransport;
import com.tlc.mcp.internal.McpServerImpl;
import com.tlc.mcp.internal.listener.McpBundleListener;
import com.tlc.mcp.internal.registry.McpServiceRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class McpAccess implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(McpAccess.class);

    private McpBundleListener bundleListener;

    @Override
    public void start(BundleContext bundleContext) {
        LOGGER.info("MCP bundle starting");

        final Configurations configurations = ConfHolder.getInstance().getProperties("mcp");
        final int workerThreads = configurations.optInt("worker.threads", 20);

        final McpServerImpl server = new McpServerImpl(configurations, workerThreads);

        final AtomicInteger workerIndex = new AtomicInteger(0);
        McpHttpTransport.setWorkerPool(Executors.newFixedThreadPool(workerThreads, r -> {
            final Thread t = new Thread(r, "mcp-worker-" + workerIndex.getAndIncrement());
            t.setDaemon(true);
            return t;
        }));

        McpServiceRegistry.getInstance().register(server);
        McpHttpTransport.setToolsResolver(server::toolsForIdentity);

        bundleListener = new McpBundleListener(
                server.getToolRegistrar(),
                server.getResourceRegistrar(),
                server.getPromptRegistrar());
        bundleContext.addBundleListener(bundleListener);

        LOGGER.info("MCP bundle started");
    }

    @Override
    public void stop(BundleContext bundleContext) {
        LOGGER.info("MCP bundle stopping");
        McpHttpTransport.shutdownWorkerPool();
        if (bundleListener != null) {
            bundleContext.removeBundleListener(bundleListener);
        }
        final McpService service = McpServiceRegistry.getInstance().unregister();
        if (service != null) {
            service.stop();
        }
        LOGGER.info("MCP bundle stopped");
    }
}

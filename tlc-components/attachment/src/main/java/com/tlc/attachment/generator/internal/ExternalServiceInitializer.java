package com.tlc.attachment.generator.internal;

import com.tlc.commons.util.ConfHolder;

/**
 * Singleton lifecycle manager responsible for initializing and shutting down
 * external services used by the attachment generation module.
 *
 * <p>This class provides a centralized entry point to manage the lifecycle of
 * resources that require explicit initialization and cleanup during application
 * startup and shutdown.</p>
 *
 * <p>Currently, it manages the lifecycle of {@link PlaywrightService}, including
 * browser engine initialization and resource cleanup.</p>
 *
 * <p>The singleton instance is created lazily using the initialization-on-demand
 * holder pattern, ensuring thread-safe initialization without explicit synchronization.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ExternalServiceInitializer {

    private static final String SERVICES = "services";
    private static final String PLAYWRIGHT_SERVICE = "playwright-service";

    private final PlaywrightService playwrightService;
    private final ConfHolder confHolder;

    private ExternalServiceInitializer() {
        playwrightService = PlaywrightService.getInstance();
        confHolder = ConfHolder.getInstance();
    }

    private static final class InstanceHolder {
        private static final ExternalServiceInitializer EXTERNAL_SERVICE_STOPPER = new ExternalServiceInitializer();
    }

    /**
     * Returns the singleton instance of {@link ExternalServiceInitializer}.
     *
     * @return shared external service stopper instance.
     */
    public static ExternalServiceInitializer getInstance() {
        return InstanceHolder.EXTERNAL_SERVICE_STOPPER;
    }

    /**
     * Initializes all managed external services.
     *
     * <p>This method delegates initialization to the underlying service managers.
     * It should be invoked during application startup before using services that
     * require external resources.</p>
     *
     * <p>Currently, this initializes the Playwright browser instance required
     * for HTML to PDF generation.</p>
     */
    public void initialize() {
        if (confHolder.getProperties(SERVICES).containsValue(PLAYWRIGHT_SERVICE)) {
            playwrightService.init();
        }
    }

    /**
     * Stops all managed external services and releases their resources.
     *
     * <p>This method delegates the shutdown operation to the corresponding service lifecycle
     * manager. It should be invoked during application shutdown to ensure resources such as
     * browser processes and client connections are properly released.</p>
     */
    public void stop() {
        if (confHolder.getProperties(SERVICES).containsValue(PLAYWRIGHT_SERVICE)) {
            playwrightService.stop();
        }
    }
}

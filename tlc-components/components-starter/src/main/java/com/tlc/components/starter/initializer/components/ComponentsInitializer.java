package com.tlc.components.starter.initializer.components;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.components.starter.conf.ApplicationConfLoader;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responsible for triggering the initialization process of the component services within the application.
 * <br/><br/>
 * <span style="color:red"><b>Warning:</b></span>
 * <ul>
 *     <li>
 *         The module initialization is critical and must be maintained meticulously. Any alterations to the order
 *         of initialization can lead to significant issues.
 *     </li>
 *     <li>
 *         Prior to modifying, it is imperative to meticulously verify the order of module
 *         initialization to mitigate potential problems and ensure smooth operation of the application.
 *     </li>
 * <ul>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public final class ComponentsInitializer extends AbstractModuleInitializer {

    @Autowired
    private ApplicationConfLoader applicationConfLoader;
    @Autowired
    private CommonsInitializer commonsInitializer;
    @Autowired
    private CacheInitializer cacheInitializer;
    @Autowired
    private I18nInitializer i18nInitializer;
    @Autowired
    private WebInitializer webInitializer;
    @Autowired
    private WebClientInitializer webClientInitializer;
    @Autowired
    private SqlInitializer sqlInitializer;
    @Autowired
    private AttachmentInitializer attachmentInitializer;
    @Autowired
    private ValidatorInitializer validatorInitializer;
    @Autowired
    private SchedulerInitializer schedulerInitializer;
    @Autowired
    private QueueInitializer queueInitializer;
    @Autowired
    private CdcCoreInitializer cdcCoreInitializer;
    @Autowired
    private ClusterInitializer clusterInitializer;
    @Autowired
    private SearchInitializer searchInitializer;
    @Autowired
    private SyncServiceInitializer syncServiceInitializer;
    @Autowired
    private NotificationInitializer notificationInitializer;
    @Autowired
    private ExchangeRateInitializer exchangeRateInitializer;
    @Autowired
    private PaymentGatewayInitializer paymentGatewayInitializer;
    @Autowired
    private IdentityServiceInitializer  identityServiceInitializer;
    @Autowired
    private McpInitializer mcpInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws IOException {
        commonsInitializer.initialize();
        cacheInitializer.initialize();
        i18nInitializer.initialize();
        validatorInitializer.initialize();
        attachmentInitializer.initialize();
        sqlInitializer.initialize();
        schedulerInitializer.initialize();
        webClientInitializer.initialize();
        webInitializer.initialize();
        queueInitializer.initialize();
        cdcCoreInitializer.initialize();
        clusterInitializer.initialize();
        searchInitializer.initialize();
        syncServiceInitializer.initialize();
        notificationInitializer.initialize();
        exchangeRateInitializer.initialize();
        paymentGatewayInitializer.initialize();
        identityServiceInitializer.initialize();
        mcpInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() throws Exception {
        commonsInitializer.postInitialize();
        i18nInitializer.postInitialize();
        cdcCoreInitializer.postInitialize();
        webClientInitializer.postInitialize();
        webInitializer.postInitialize();
        searchInitializer.postInitialize();
        notificationInitializer.postInitialize();
        exchangeRateInitializer.postInitialize();
        attachmentInitializer.postInitialize();
        mcpInitializer.postInitialize();
    }

    public void startClusterRecoveryMonitoring() {
        webClientInitializer.startRecoveryMonitoring();
    }

    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return null;
    }

    /**
     * Gracefully shuts down all component services in reverse order of initialization.
     * Each service's stop is attempted independently — failure in one does not prevent others from stopping.
     */
    @Override
    public void shutdown() {
        LOGGER.info("ComponentsInitializer: Shutting down component services...");

        stopQuietly("IdentityService", identityServiceInitializer::shutdown);
        stopQuietly("PaymentGateway", paymentGatewayInitializer::shutdown);
        stopQuietly("ExchangeRate", exchangeRateInitializer::shutdown);
        stopQuietly("Notification", notificationInitializer::shutdown);
        stopQuietly("SyncService", syncServiceInitializer::shutdown);
        stopQuietly("Search", searchInitializer::shutdown);
        stopQuietly("Cluster", clusterInitializer::shutdown);
        stopQuietly("Queue", queueInitializer::shutdown);
        stopQuietly("Web", webInitializer::shutdown);
        stopQuietly("WebClient", webClientInitializer::shutdown);
        stopQuietly("Scheduler", schedulerInitializer::shutdown);
        stopQuietly("Sql", sqlInitializer::shutdown);
        stopQuietly("Attachment", attachmentInitializer::shutdown);
        stopQuietly("Cache", cacheInitializer::shutdown);
        stopQuietly("Commons", commonsInitializer::shutdown);

        LOGGER.info("ComponentsInitializer: All component services stopped");
    }

    /**
     * Executes a shutdown action, catching and logging any exception without propagating it.
     */
    private void stopQuietly(final String serviceName, final Runnable shutdownAction) {
        try {
            shutdownAction.run();
            LOGGER.info("ComponentsInitializer: '{}' stopped", serviceName);
        } catch (Exception exception) {
            LOGGER.error("ComponentsInitializer: Failed to stop '{}'. Reason: {}", serviceName, exception.getMessage(), exception);
        }
    }

}

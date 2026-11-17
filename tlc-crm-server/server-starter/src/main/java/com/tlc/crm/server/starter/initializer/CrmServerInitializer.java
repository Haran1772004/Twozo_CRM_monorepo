package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.auto.service.AutoServiceRegistry;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the crm-server services within the application.
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
public final class CrmServerInitializer extends AbstractModuleInitializer {

    @Autowired
    private CommonInitializer commonInitializer;
    @Autowired
    private IconInitializer iconInitializer;
    @Autowired
    private EntityInitializer entityInitializer;
    @Autowired
    private WebFormInitializer webFormInitializer;
    @Autowired
    private CoreInitializer coreInitializer;
    @Autowired
    private NotificationsInitializer notificationsInitializer;
    @Autowired
    private WorkflowInitializer workflowInitializer;
    @Autowired
    private FieldsInitializer fieldsInitializer;
    @Autowired
    private SecurityInitializer securityInitializer;
    @Autowired
    private EmailNotificationInitializer emailNotificationInitializer;
    @Autowired
    private MessengerInitializer messengerInitializer;
    @Autowired
    private ZapierInitializer zapierInitializer;
    @Autowired
    private AutoAssignmentInitializer autoAssignmentInitializer;
    @Autowired
    private AnalyticsInitializer analyticsInitializer;
    @Autowired
    private EmailLinkerInitializer emailLinkerInitializer;
    @Autowired
    private MarketplaceInitializer marketplaceInitializer;
    @Autowired
    private SubscriptionInitializer subscriptionInitializer;
    @Autowired
    private AIInitializer aiInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        try {
            securityInitializer.initialize();
            fieldsInitializer.initialize();
            commonInitializer.initialize();
            iconInitializer.initialize();
            entityInitializer.initialize();
            webFormInitializer.initialize();
            emailNotificationInitializer.initialize();
            coreInitializer.initialize();
            autoAssignmentInitializer.initialize();
            notificationsInitializer.initialize();
            workflowInitializer.initialize();
            messengerInitializer.initialize();
            zapierInitializer.initialize();
            analyticsInitializer.initialize();
            emailLinkerInitializer.initialize();
            marketplaceInitializer.initialize();
            subscriptionInitializer.initialize();
            aiInitializer.initialize();
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "CRM server initialization failed: " + exception.getMessage());
        }

        LOGGER.info("Application Initializer: Pre initialization of CRM server is completed");
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() throws Exception {
        loadAutoServiceRegistry();
        securityInitializer.postInitialize();
        fieldsInitializer.postInitialize();
        commonInitializer.postInitialize();
        marketplaceInitializer.postInitialize();
        analyticsInitializer.postInitialize();
        entityInitializer.postInitialize();
        coreInitializer.postInitialize();
        autoAssignmentInitializer.postInitialize();
        emailNotificationInitializer.postInitialize();
        notificationsInitializer.postInitialize();
        workflowInitializer.postInitialize();
        messengerInitializer.postInitialize();
        zapierInitializer.postInitialize();
        emailLinkerInitializer.postInitialize();
        subscriptionInitializer.postInitialize();
        aiInitializer.postInitialize();
        LOGGER.info("Application Initializer: Post initialization of CRM server is completed");
    }

    /**
     * <p>
     * Loads {@link AutoServiceRegistry} impl modules.
     * </p>
     */
    private void loadAutoServiceRegistry() {
        try {
            loadAutoServiceRegistry("com.tlc");
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, "Unable to register AutoService and it's implementations");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }
}

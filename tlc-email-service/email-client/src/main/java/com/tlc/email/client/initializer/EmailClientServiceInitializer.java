package com.tlc.email.client.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.service.EmailAddOnService;
import com.tlc.email.server.controller.DeleteRequestController;
import com.tlc.email.server.controller.DomainRequestController;
import com.tlc.email.server.controller.SendRequestController;
import com.tlc.email.server.controller.UpdateRequestController;
import com.tlc.email.server.controller.webhook.WebhookReceiverRenewalService;
import com.tlc.email.server.controller.webhook.WebhookReceiverService;
import com.tlc.email.server.controller.TransactionalMailLoadService;
import com.tlc.email.server.controller.ManualSyncService;
import com.tlc.email.server.controller.InboundEmailRetryService;
import com.tlc.email.server.controller.InboundEmailService;
import com.tlc.email.server.internal.provider.DomainAuthenticatedEmailServiceProvider;
import com.tlc.email.server.service.AutoSyncService;
import com.tlc.email.server.service.BackgroundSyncService;

/**
 * <p>
 * Initialization process is triggered during the application startup. Ensure that
 * all required properties and configurations for the email service are defined
 * before the application context is loaded to prevent initialization errors.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ServiceInitializer
 */
public final class EmailClientServiceInitializer implements ServiceInitializer {

    private final ManualSyncService manualSyncService;
    private final InboundEmailService inboundEmailService;
    private final InboundEmailRetryService inboundEmailRetryService;
    private final TransactionalMailLoadService transactionalMailLoadService;
    private final SendRequestController sendRequestController;
    private final UpdateRequestController updateRequestController;
    private final DeleteRequestController deleteRequestController;
    private final WebhookReceiverService webhookReceiverService;
    private final WebhookReceiverRenewalService webhookReceiverRenewalService;
    private final AutoSyncService autoSyncService;
    private final BackgroundSyncService backgroundSyncService;
    private final DomainRequestController domainRequestController;

    private EmailClientServiceInitializer() {
        this.manualSyncService = new ManualSyncService();
        this.inboundEmailService = InboundEmailService.getInstance();
        this.inboundEmailRetryService = InboundEmailRetryService.getInstance();
        this.transactionalMailLoadService = TransactionalMailLoadService.getInstance();
        this.sendRequestController = SendRequestController.getInstance();
        this.updateRequestController = UpdateRequestController.getInstance();
        this.deleteRequestController = DeleteRequestController.getInstance();
        this.webhookReceiverService = WebhookReceiverService.getInstance();
        this.webhookReceiverRenewalService = WebhookReceiverRenewalService.getInstance();
        this.autoSyncService = AutoSyncService.getInstance();
        this.backgroundSyncService = BackgroundSyncService.getInstance();
        this.domainRequestController = DomainRequestController.getInstance();
    }

    private static final class Instance {
        private static final EmailClientServiceInitializer EMAIL_SERVICE_INITIALIZER = new EmailClientServiceInitializer();
    }

    public static EmailClientServiceInitializer getInstance() {
        return Instance.EMAIL_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        DomainAuthenticatedEmailServiceProvider.getInstance().start(configurations.getNestedConfigs("mailService"));
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        final Configurations executorConfigurations = configurations.getNestedConfigs(EmailFields.EXECUTOR);

        manualSyncService.start(executorConfigurations);
        inboundEmailService.start(executorConfigurations);
        inboundEmailRetryService.start(executorConfigurations);
        transactionalMailLoadService.start(executorConfigurations);
        sendRequestController.start(executorConfigurations);
        updateRequestController.start(executorConfigurations);
        deleteRequestController.start(executorConfigurations);
        webhookReceiverService.start(executorConfigurations);
        webhookReceiverRenewalService.start(executorConfigurations);
        autoSyncService.start(executorConfigurations);
        domainRequestController.start(configurations);
        backgroundSyncService.start(null);

        EmailAddOnService.init((configurations.getNestedConfigs(EmailFields.ADD_ON_SERVICES)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        manualSyncService.stop();
        inboundEmailService.stop();
        inboundEmailRetryService.stop();
        transactionalMailLoadService.stop();
        sendRequestController.stop();
        updateRequestController.stop();
        deleteRequestController.stop();
        webhookReceiverService.stop();
        webhookReceiverRenewalService.stop();
        autoSyncService.stop();
        backgroundSyncService.stop();
        domainRequestController.stop();
    }
}
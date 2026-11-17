package com.tlc.crm.email.core.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.email.core.controller.AutoSyncDataLoadService;
import com.tlc.crm.email.core.controller.EmailDomainRequestActionController;
import com.tlc.crm.email.core.controller.InboundEmailDataLoadService;
import com.tlc.crm.email.core.controller.SyncDataLoadService;
import com.tlc.crm.email.core.controller.EmailSendDataLoaderService;
import com.tlc.crm.email.core.controller.SyncStatusController;

/**
 * <p>
 * An initializer that email core service which needs to be started.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailCoreInitializer implements ServiceInitializer {

    private final SyncDataLoadService syncDataLoadService;
    private final SyncStatusController syncStatusController;
    private final EmailSendDataLoaderService emailSendDataLoaderService;
    private final InboundEmailDataLoadService inboundEmailDataLoadService;
    private final AutoSyncDataLoadService autoSyncDataLoadService;
    private final EmailDomainRequestActionController emailDomainRequestActionController;

    private EmailCoreInitializer() {
        this.syncStatusController = SyncStatusController.getInstance();
        syncDataLoadService = new SyncDataLoadService();
        emailSendDataLoaderService = new EmailSendDataLoaderService();
        inboundEmailDataLoadService = new InboundEmailDataLoadService();
        autoSyncDataLoadService = new AutoSyncDataLoadService();
        this.emailDomainRequestActionController = new EmailDomainRequestActionController();
    }

    private static final class Instance {
        private static final EmailCoreInitializer EMAIL_CORE_SERVICE_INITIALIZER = new EmailCoreInitializer();
    }

    public static EmailCoreInitializer getInstance() {
        return EmailCoreInitializer.Instance.EMAIL_CORE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        emailSendDataLoaderService.start(null);
        syncStatusController.start(null);
        syncDataLoadService.start(configurations);
        inboundEmailDataLoadService.start(configurations);
        autoSyncDataLoadService.start(configurations);
        emailDomainRequestActionController.start(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        emailSendDataLoaderService.stop();
        syncStatusController.stop();
        syncDataLoadService.stop();
        inboundEmailDataLoadService.stop();
        autoSyncDataLoadService.stop();
        emailDomainRequestActionController.stop();
    }
}
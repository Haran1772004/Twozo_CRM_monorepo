package com.tlc.sync.service.oauth.internal;

import com.tlc.commons.util.Configurations;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.service.SyncInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Implements the services required for handling OAuth integrations, including configuration.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SyncInitializationServiceImpl implements SyncInitializationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncInitializationServiceImpl.class);

    public SyncInitializationServiceImpl() {
    }

    /**
     * {@inheritDoc}
     *
     * @param appConfigurations Represents the app configurations
     */

    @Override
    public void start(final Configurations appConfigurations) {
        final SyncServiceProvider syncServiceProvider = SyncServiceProvider.getInstance();
        final Configurations gmailConfigurations = appConfigurations.getNestedConfigs(SyncServiceFields.GMAIL);
        final Configurations outlookConfigurations = appConfigurations.getNestedConfigs(SyncServiceFields.OUTLOOK);
        final Configurations facebookConfigurations = appConfigurations.getNestedConfigs(SyncServiceFields.FACEBOOK);
        final Configurations instagramConfigurations = appConfigurations.getNestedConfigs(SyncServiceFields.INSTAGRAM);
        final Configurations whatsappConfigurations = appConfigurations.getNestedConfigs(SyncServiceFields.WHATSAPP);

        if (!gmailConfigurations.isEmpty()) {
            /* Email */
            syncServiceProvider.setEmailGoogleProperties(gmailConfigurations);
            LOGGER.info("Sync service: The email sync service related configurations are loaded successfully. Provider: Gmail");

            /* Calendar */
            syncServiceProvider.setCalendarGmailProperties(gmailConfigurations);
            LOGGER.info("Sync service: The calendar sync service related configurations are loaded successfully. Provider: Gmail");

            /* Contact */
            syncServiceProvider.setContactGmailProperties(gmailConfigurations);
            LOGGER.info("Sync service: The contact sync service related configurations are loaded successfully. Provider: Gmail");
        }

        if (!outlookConfigurations.isEmpty()) {
            /* Email */
            syncServiceProvider.setEmailOutlookProperties(outlookConfigurations);
            LOGGER.info("Sync service: The email sync service  related configurations are loaded successfully. Provider: Outlook");

            /* Calendar */
            syncServiceProvider.setCalendarOutlookProperties(outlookConfigurations);
            LOGGER.info("Sync service: The calendar sync service related configurations are loaded successfully. Provider: Outlook");

            /* Contact */
            syncServiceProvider.setContactOutlookProperties(outlookConfigurations);
            LOGGER.info("Sync service: The contact sync service related configurations are loaded successfully. Provider: Outlook");
        }

        /* Facebook */
        if (!facebookConfigurations.isEmpty()) {
            syncServiceProvider.setFacebookProperties(facebookConfigurations);
            LOGGER.info("Sync service: The Facebook sync service related configurations are loaded successfully");
        }

        /* Instagram */
        if (!instagramConfigurations.isEmpty()) {
            syncServiceProvider.setInstagramProperties(instagramConfigurations);
            LOGGER.info("Sync service: The Instagram sync service related configurations are loaded successfully");
        }

        /* WhatsApp */
        if (!whatsappConfigurations.isEmpty()) {
            syncServiceProvider.setWhatsAppProperties(whatsappConfigurations);
            LOGGER.info("Sync service: The WhatsApp sync service related configurations are loaded successfully");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        LOGGER.info("Sync: Sync service is stopped");
    }
}

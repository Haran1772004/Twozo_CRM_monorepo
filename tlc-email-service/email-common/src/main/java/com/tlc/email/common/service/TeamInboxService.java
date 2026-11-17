package com.tlc.email.common.service;

import com.tlc.commons.util.Configurations;
import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.model.ServiceType;

/**
 * <p>
 * Provides the Team Inbox email service, responsible for initialization and service type identification.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class TeamInboxService extends AbstractAddOnService {

    private TeamInboxService() {
    }

    private static class Instance {
        private static final EmailAddOnService INSTANCE = new TeamInboxService();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link TeamInboxService}.
     * </p>
     *
     * @return Singleton instance of {@link EmailAddOnService}
     */
    public static EmailAddOnService getInstance() {
        return TeamInboxService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Starts the Team Inbox service by initializing configuration values.
     * </p>
     *
     * @param configurations Application-level {@link Configurations} containing service-specific settings
     */
    @Override
    public void start(final Configurations configurations) {
        this.prefix = configurations.getNestedConfigs(EmailFields.TEAM_INBOX)
                .getConfig(EmailFields.ALIAS_PREFIX);
        super.start(configurations);
    }

    /**
     * <p>
     * Returns the service type for this add-on service.
     * </p>
     *
     * @return {@link ServiceType} enum representing the Team Inbox service type
     */
    @Override
    public ServiceType getServiceType() {
        return ServiceType.TEAM_INBOX;
    }
}
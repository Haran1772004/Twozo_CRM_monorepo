package com.tlc.crm.notifications.settings;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.iam.manager.UserNotificationSettingsManager;
import com.tlc.crm.notification.app.model.UserNotificationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Performs the action used to create the default notification settings for the particular user in an organization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@OrgInitiator(name = "notificationSettings")
public class NotificationSettingsInitializer implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSettingsInitializer.class);

    private final UserNotificationSettingsManager userNotificationSettingsManager;

    public NotificationSettingsInitializer() {
        this.userNotificationSettingsManager = UserNotificationSettingsManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();

        LOGGER.info("Org Init Action: Notification default settings storage initialization started. Org ID: {}, User ID: {}", orgId, userId);
        final UserNotificationSettings userNotificationSettings = new UserNotificationSettings();

        userNotificationSettings.setOrgId(orgId);
        userNotificationSettings.setUserId(userId);
        userNotificationSettingsManager.loadDefaultSettings(userNotificationSettings);
        LOGGER.info("Org Init Action: Notification default settings storage initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}

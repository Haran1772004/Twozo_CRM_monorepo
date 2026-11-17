package com.tlc.crm.email.notification.email.action;

import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.internal.async.service.EmailSyncSettingsService;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;

/**
 * <p>
 * Provides an abstract implementation for email synchronization actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractEmailSyncAction extends CrmSecureAction {

    protected final EmailSyncSettingsService emailSyncSettingsService;

    /**
     * <p>
     * Initializes the email sync service, configuration, manager, and redirect handler instances.
     * </p>
     */
    protected AbstractEmailSyncAction() {
        emailSyncSettingsService = EmailSyncSettingsService.getInstance();
    }

    /**
     * <p>
     * Initiates the email synchronization process for a given organization and user.
     * </p>
     *
     * @param orgId           The unique identifier of the organization.
     * @param userId          The unique identifier of the user.
     * @param syncAddressInfo Contains provider-specific email address details.
     * @param emailProvider   Represents the email provider
     */
    protected void initiateEmailSync(final Long orgId, final Long userId, final SyncAddressInfo syncAddressInfo,
                                     final EmailProvider emailProvider) {
        final EmailSyncSettings syncSettings = new EmailSyncSettings();

        syncSettings.setOrgId(orgId);
        syncSettings.setUserId(userId);
        syncSettings.setConnectionStatus(ConnectionStatus.ACTIVE);
        syncSettings.setAddress(syncAddressInfo);
        syncSettings.setSyncStatus(SyncStatus.NEW);
        syncSettings.setEmailProvider(emailProvider);

        this.emailSyncSettingsService.upsert(orgId, userId, syncSettings);
    }
}
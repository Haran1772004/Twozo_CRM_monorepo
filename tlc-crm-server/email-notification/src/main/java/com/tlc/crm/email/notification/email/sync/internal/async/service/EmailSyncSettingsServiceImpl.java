package com.tlc.crm.email.notification.email.sync.internal.async.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.sync.SyncSettings;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.internal.async.resolver.EmailSyncSettingResolver;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the service implementation for managing {@link EmailSyncSettings}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailSyncSettingsServiceImpl implements EmailSyncSettingsService, SecureUserTaskProcessor {

    private final EmailSyncSettingResolver emailSyncSettingResolver;
    private final EmailSyncManager emailSyncManager;

    private EmailSyncSettingsServiceImpl() {
        this.emailSyncSettingResolver = EmailSyncSettingResolver.getInstance();
        this.emailSyncManager = EmailSyncManager.getInstance();
    }

    private static final class Instance {
        private static final EmailSyncSettingsService INSTANCE = new EmailSyncSettingsServiceImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingsService}.
     * </p>
     *
     * @return the {@link EmailSyncSettingsService} instance
     */
    public static EmailSyncSettingsService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Retrieves all {@link EmailSyncSettings} for the given organization and user.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    @Override
    public Collection<EmailSyncSettings> getSyncSettings(final long orgId, final long userId) {
        final Collection<Long> syncIds = emailSyncManager.getSyncIds(orgId, userId);

        return emailSyncSettingResolver.resolveAll(orgId, userId, syncIds);
    }

    /**
     * <p>
     * Retrieves all {@link EmailSyncSettings} for the given organization, user and sync statuses.
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param userId       Represents the user id
     * @param syncStatuses Represents the collection of {@link SyncStatus}
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    @Override
    public Collection<EmailSyncSettings> getSyncSettings(final long orgId, final long userId, final Collection<SyncStatus> syncStatuses) {
        final Collection<EmailSyncSettings> emailSyncSettings = getSyncSettings(orgId, userId);

        return emailSyncSettings.stream()
                .filter(syncSetting -> syncStatuses.contains(syncSetting.getSyncStatus()))
                .collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves all {@link EmailSyncSettings} for the given email address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    @Override
    public Collection<EmailSyncSettings> getSyncSettings(final String emailAddress) {
        final Collection<EmailSyncSettings> partialSettings = emailSyncManager.getPartialSettings(emailAddress);
        final Collection<EmailSyncSettings> settings = new HashSet<>();

        partialSettings.forEach(emailSyncSettings -> {
            final long orgId = emailSyncSettings.orgId();
            final long userId = emailSyncSettings.getUserId();

            secureProcess(orgId, userId, () ->
                    getSyncSetting(emailSyncSettings.orgId(), emailSyncSettings.getUserId(), emailSyncSettings.id()).ifPresent(settings::add)
            );
        });

        return settings;
    }

    /**
     * <p>
     * Checks whether the sync is active for the given organization, user and sync id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param syncId Represents the sync configuration id
     * @return true if the sync is active, false otherwise
     */
    @Override
    public boolean isSyncActive(final long orgId, final long userId, final long syncId) {
        return false;
    }

    /**
     * <p>
     * Retrieves the {@link EmailSyncSettings} for the given organization, user and sync id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param syncId Represents the sync configuration id
     * @return {@link Optional} of {@link EmailSyncSettings}
     */
    @Override
    public Optional<EmailSyncSettings> getSyncSetting(final long orgId, final long userId, final long syncId) {
        return emailSyncSettingResolver.resolve(orgId, userId, syncId);
    }

    /**
     * <p>
     * Retrieves the {@link EmailSyncSettings} for the given organization, user and email address.
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param userId       Represents the user id
     * @param emailAddress Represents the email address
     * @return {@link Optional} of {@link EmailSyncSettings}
     */
    @Override
    public Optional<EmailSyncSettings> getSyncSetting(final long orgId, final long userId, final String emailAddress) {
        final Collection<EmailSyncSettings> emailSyncSettings = getSyncSettings(orgId, userId);

        return emailSyncSettings.stream()
                .filter(setting -> Objects.equals(setting.getAddress().getEmailAddress(), emailAddress))
                .findFirst();
    }

    /**
     * <p>
     * Retrieves the {@link SyncAddressInfo} for the given organization, user and sync id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param syncId Represents the sync configuration id
     * @return {@link Optional} of {@link SyncAddressInfo}
     */
    @Override
    public Optional<SyncAddressInfo> getSyncAddressInfo(final long orgId, final long userId, final long syncId) {
        return getSyncSetting(orgId, userId, syncId).map(SyncSettings::getAddress);
    }

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} for the given organization.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param emailSyncSettings the {@link EmailSyncSettings}
     */
    @Override
    public void update(final long orgId, final EmailSyncSettings emailSyncSettings) {
        emailSyncSettingResolver.update(orgId, List.of(emailSyncSettings));
    }

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} for the given organization.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param emailSyncSettings Collections of {@link EmailSyncSettings}
     */
    @Override
    public void update(final long orgId, final Collection<EmailSyncSettings> emailSyncSettings) {
        emailSyncSettingResolver.update(orgId, emailSyncSettings);
    }

    /**
     * <p>
     * Checks whether the {@link EmailSyncSettings} exists for the given organization, user and sync id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param syncId Represents the sync configuration id
     * @return true if the sync settings exists, false otherwise
     */
    @Override
    public boolean exists(final long orgId, final long userId, final long syncId) {
        return getSyncSetting(orgId, userId, syncId).isPresent();
    }

    /**
     * <p>
     * Inserts or updates the {@link EmailSyncSettings} for the given organization and user.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user id
     * @param emailSyncSettings the {@link EmailSyncSettings}
     */
    @Override
    public void upsert(final long orgId, final long userId, final EmailSyncSettings emailSyncSettings) {
        final Optional<EmailSyncSettings> sourceSyncSettings = getSyncSetting(orgId, userId, emailSyncSettings.getAddress().getEmailAddress());

        emailSyncSettings.setEmailSyncConfig(getEmailSyncConfig());

        if (sourceSyncSettings.isEmpty()) {
            emailSyncManager.create(orgId, List.of(emailSyncSettings));
        } else {
            final EmailSyncSettings existingSettings = sourceSyncSettings.get();
            final ConnectionStatus connectionStatus = existingSettings.getConnectionStatus();

            if (existingSettings.isSynced() || Objects.isNull(connectionStatus)) {
                throw ErrorCode.get(EmailErrorCodes.EMAIL_ALREADY_EXISTS, "i18n.notifications.email.sync.mail.already.exists");
            }

            switch (connectionStatus) {
                case ACTIVE ->
                        throw ErrorCode.get(EmailErrorCodes.EMAIL_ALREADY_EXISTS, "i18n.notifications.email.sync.mail.already.exists");
                case INACTIVE, REMOVED -> {
                    emailSyncSettings.setId(existingSettings.id());
                    emailSyncSettingResolver.update(orgId, List.of(emailSyncSettings));
                }
            }
        }
    }

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} based on the provided {@link SyncStatusInfo}.
     * </p>
     *
     * @param syncStatusInfo Represents the {@link SyncStatusInfo}
     */
    @Override
    public void update(final SyncStatusInfo syncStatusInfo) {
        final String emailAddress = syncStatusInfo.getEmailAddress();
        final Collection<EmailSyncSettings> emailSyncSettings = getSyncSettings(emailAddress);
        final SyncStatus syncStatus = syncStatusInfo.getSyncStatus();

        emailSyncSettings.forEach(emailSyncSetting -> {
            final long orgId = emailSyncSetting.orgId();
            final long userId = emailSyncSetting.getUserId();

            secureProcess(orgId, userId, () -> {
                emailSyncSetting.setSyncStatus(syncStatus);

                if (syncStatus.isSynced()) {
                    emailSyncSetting.clearInProgressConfig();

                    if (!emailSyncManager.hasDefaultSetting(orgId, userId)) {
                        emailSyncSetting.setDefault(true);
                    }
                }
                emailSyncSettingResolver.update(orgId, List.of(emailSyncSetting));
            });
        });
    }

    /**
     * <p>
     * Retrieves the active and active email sync settings for the given user
     * within the specified organization, by resolving each sync id and filtering
     * out settings that are not fully synced or have an inactive connection.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id whose synced email settings are to be fetched
     * @return {@link Collection} of {@link EmailSyncSettings} that are active and synced,
     *         or an empty list if no sync ids are found for the user
     */
    @Override
    public Collection<EmailSyncSettings> getActiveEmailSettings(final Long orgId, final Long userId) {
        return getSyncSettings(orgId, userId).stream().filter(setting -> setting.getSyncStatus().isActive() &&
                setting.getConnectionStatus().isActive()).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets the active sync setting for the user
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id whose synced email settings are to be fetched
     * @return {@link Collection} of {@link EmailSyncSettings} that are active and synced,
     * or an empty list if no sync ids are found for the user
     */
    @Override
    public Collection<EmailSyncSettings> getActiveSyncSettings(final Long orgId, final Long userId) {
        return getSyncSettings(orgId, userId).stream().filter(setting -> setting.getConnectionStatus()
                .isActive()).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Temporary backfill operation to load provider and default flag for synced emails.
     * </p>
     *
     * @return number of rows updated
     */
    @Override
    public int loadProviderAndIsDefault() {
        return emailSyncManager.loadProviderAndIsDefault();
    }

    /**
     * <p>
     * Temporary backfill operation to load synced email conversations from email-server.
     * </p>
     *
     * @return number of conversations loaded into OMEMSyncedEmailConversations
     */
    @Override
    public int loadSyncedEmailConversations() {
        return emailSyncManager.loadSyncedEmailConversations();
    }

    /**
     * <p>
     * Gets the default {@link EmailSyncConfig}.
     * </p>
     *
     * @return {@link EmailSyncConfig}
     */
    private EmailSyncConfig getEmailSyncConfig() {
        final short activeState = ConnectionStatus.ACTIVE.getId();
        final short inActiveState = ConnectionStatus.INACTIVE.getId();
        final EmailSyncConfig emailSyncConfig = new EmailSyncConfig();

        emailSyncConfig.setArchiveMode(activeState);
        emailSyncConfig.setDeleteMode(inActiveState);
        emailSyncConfig.setRelatedContactState(inActiveState);
        emailSyncConfig.setAllLabelState(activeState);
        emailSyncConfig.setFolderIds(Collections.emptyList());

        return emailSyncConfig;
    }
}
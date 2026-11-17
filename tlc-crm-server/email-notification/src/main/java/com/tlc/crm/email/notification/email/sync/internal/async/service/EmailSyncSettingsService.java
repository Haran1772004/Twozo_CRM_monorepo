package com.tlc.crm.email.notification.email.sync.internal.async.service;

import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.notification.model.sync.SyncStatus;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the service for managing {@link EmailSyncSettings}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailSyncSettingsService {

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingsService}.
     * </p>
     *
     * @return the {@link EmailSyncSettingsService} instance
     */
    static EmailSyncSettingsService getInstance() {
        return EmailSyncSettingsServiceImpl.getInstance();
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
    Collection<EmailSyncSettings> getSyncSettings(final long orgId, final long userId);

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
    Collection<EmailSyncSettings> getSyncSettings(final long orgId, final long userId, final Collection<SyncStatus> syncStatuses);

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
    boolean isSyncActive(final long orgId, final long userId, final long syncId);

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
    Optional<EmailSyncSettings> getSyncSetting(final long orgId, final long userId, final long syncId);

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
    Optional<EmailSyncSettings> getSyncSetting(final long orgId, final long userId, final String emailAddress);

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
    Optional<SyncAddressInfo> getSyncAddressInfo(final long orgId, final long userId, final long syncId);

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} for the given organization.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param emailSyncSettings the {@link EmailSyncSettings}
     */
    void update(final long orgId, final EmailSyncSettings emailSyncSettings);

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} for the given organization.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param emailSyncSettings the {@link EmailSyncSettings}
     */
    void update(final long orgId, final Collection<EmailSyncSettings> emailSyncSettings);

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
    boolean exists(final long orgId, final long userId, final long syncId);

    /**
     * <p>
     * Inserts or updates the {@link EmailSyncSettings} for the given organization and user.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param userId            Represents the user id
     * @param emailSyncSettings the {@link EmailSyncSettings}
     */
    void upsert(final long orgId, final long userId, final EmailSyncSettings emailSyncSettings);

    /**
     * <p>
     * Retrieves all {@link EmailSyncSettings} for the given email address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    Collection<EmailSyncSettings> getSyncSettings(final String emailAddress);

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} based on the provided {@link SyncStatusInfo}.
     * </p>
     *
     * @param syncStatusInfo Represents the {@link SyncStatusInfo}
     */
    void update(final SyncStatusInfo syncStatusInfo);

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
     * or an empty list if no sync ids are found for the user
     */
    Collection<EmailSyncSettings> getActiveEmailSettings(final Long orgId, final Long userId);

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
    Collection<EmailSyncSettings> getActiveSyncSettings(final Long orgId, final Long userId);

    /**
     * <p>
     * Temporary backfill operation to load provider and default flag for synced emails.
     * </p>
     *
     * @return number of rows updated
     */
    int loadProviderAndIsDefault();

    /**
     * <p>
     * Temporary backfill operation to load synced email conversations from email-server.
     * </p>
     *
     * @return number of conversations loaded into OMEMSyncedEmailConversations
     */
    int loadSyncedEmailConversations();
}
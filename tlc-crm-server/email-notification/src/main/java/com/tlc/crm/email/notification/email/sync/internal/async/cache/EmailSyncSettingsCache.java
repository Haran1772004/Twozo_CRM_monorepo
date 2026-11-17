package com.tlc.crm.email.notification.email.sync.internal.async.cache;

import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provide the cache service for email sync settings.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailSyncSettingsCache {

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingsCache}.
     * </p>
     *
     * @return the {@link EmailSyncSettingsCache} instance
     */
    static EmailSyncSettingsCache getInstance() {
        return EmailSyncSettingCacheImpl.getInstance();
    }

    /**
     * <p>
     * Put EmailSyncSettings into cache keyed by syncId.
     * </p>
     *
     * @param syncId   sync configuration id
     * @param settings the {@link EmailSyncSettings}
     */
    void put(final Long syncId, final EmailSyncSettings settings);

    /**
     * <p>
     * Get EmailSyncSettings from cache for the given syncId.
     * </p>
     *
     * @param syncId sync configuration id
     * @return the {@link EmailSyncSettings}
     */
    Optional<EmailSyncSettings> get(final Long syncId);

    /**
     * <p>
     * Remove cached entry for the given syncId.
     * </p>
     *
     * @param syncId sync configuration id
     */
    void remove(final Long syncId);

    /**
     * <p>
     * Remove cached entries for the given syncIds.
     * </p>
     *
     * @param syncIds sync configuration ids
     */
    void remove(final Collection<Long> syncIds);

    /**
     * <p>
     * Clear the whole cache.
     * </p>
     */
    void clear();
}
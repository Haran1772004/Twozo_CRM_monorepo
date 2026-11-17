package com.tlc.crm.email.notification.email.sync.internal.async.resolver;

import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the resolver service to fetch and update {@link EmailSyncSettings}
 * using cache and manager services.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface EmailSyncSettingResolver {

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingResolver}.
     * </p>
     *
     * @return the {@link EmailSyncSettingResolver} instance
     */
    static EmailSyncSettingResolver getInstance() {
        return EmailSyncSettingResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link EmailSyncSettings} by organization and sync id.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param syncId Represents the sync configuration id
     * @return {@link Optional} of {@link EmailSyncSettings}
     */
    Optional<EmailSyncSettings> resolve(final long orgId, final long userId, final Long syncId);

    /**
     * <p>
     * Updates the {@link EmailSyncSettings} in persistence and refreshes the cache.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param settings the {@link EmailSyncSettings}
     */
    void update(final long orgId, final Collection<EmailSyncSettings> settings);

    /**
     * <p>
     * Removes the cached entry for the given sync id.
     * </p>
     *
     * @param syncId Represents the sync configuration id
     */
    void remove(final Long syncId);

    /**
     * <p>
     * Resolves the collection of {@link EmailSyncSettings} by organization and sync ids.
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param userId  Represents the user id
     * @param syncIds Represents the collection of sync configuration ids
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    Collection<EmailSyncSettings> resolveAll(final long orgId, final long userId, final Collection<Long> syncIds);

    /**
     * <p>
     * Removes the cached entries for the given sync ids.
     * </p>
     *
     * @param syncIds Represents the sync configuration ids
     */
    void remove(Collection<Long> syncIds);
}
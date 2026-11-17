package com.tlc.crm.email.notification.email.sync.internal.async.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the cache implementation for email sync settings.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailSyncSettingCacheImpl implements EmailSyncSettingsCache {

    private final Cache<Long, EmailSyncSettings> emailSyncSettingsCache;

    private EmailSyncSettingCacheImpl() {
        final CacheConfig<Long, EmailSyncSettings> config = new CacheConfig<>(EmailFields.EMAIL_SYNC_SETTINGS.getName());

        config.setTtl((int) TimeUnit.MINUTES.toMillis(30));
        this.emailSyncSettingsCache = CacheServiceProvider.get().createOrOpenCache(config);
    }

    private static final class InstanceHolder {
        private static final EmailSyncSettingCacheImpl INSTANCE = new EmailSyncSettingCacheImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingsCache}.
     * </p>
     *
     * @return the {@link EmailSyncSettingsCache} instance
     */
    public static EmailSyncSettingsCache getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Put EmailSyncSettings into cache keyed by syncId.
     * </p>
     *
     * @param syncId   sync configuration id
     * @param settings the {@link EmailSyncSettings}
     */
    @Override
    public void put(final Long syncId, final EmailSyncSettings settings) {
        if (Objects.isNull(syncId) || Objects.isNull(settings)) {
            return;
        }
        emailSyncSettingsCache.put(syncId, settings);
    }

    /**
     * <p>
     * Get EmailSyncSettings from cache for the given syncId.
     * </p>
     *
     * @param syncId sync configuration id
     * @return the {@link EmailSyncSettings} or null if not present
     */
    @Override
    public Optional<EmailSyncSettings> get(final Long syncId) {
        if (Objects.isNull(syncId)) {
            return Optional.empty();
        }

        return Optional.ofNullable(emailSyncSettingsCache.get(syncId));
    }

    /**
     * <p>
     * Remove cached entry for the given syncId.
     * </p>
     *
     * @param syncId sync configuration id
     */
    @Override
    public void remove(final Long syncId) {
        if (Objects.isNull(syncId)) {
            return;
        }
        emailSyncSettingsCache.remove(syncId);
    }

    /**
     * <p>
     * Remove cached entries for the given syncIds.
     * </p>
     *
     * @param syncIds sync configuration ids
     */
    @Override
    public void remove(final Collection<Long> syncIds) {
        if (Objects.isNull(syncIds) || syncIds.isEmpty()) {
            return;
        }
        final Set<Long> validSyncIds = syncIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (validSyncIds.isEmpty()) {
            return;
        }
        emailSyncSettingsCache.removeAll(validSyncIds);
    }

    /**
     * <p>
     * Clear the whole cache.
     * </p>
     */
    @Override
    public void clear() {
        emailSyncSettingsCache.clear();
    }
}
package com.tlc.crm.email.notification.email.sync.internal.async.resolver;

import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.email.sync.internal.async.cache.EmailSyncSettingsCache;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the resolver implementation for {@link EmailSyncSettings}
 * using cache and manager services.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EmailSyncSettingResolverImpl implements EmailSyncSettingResolver {

    private final EmailSyncSettingsCache cache;
    private final ReentrantReadWriteLock lock;
    private final EmailSyncManager emailSyncManager;

    private EmailSyncSettingResolverImpl() {
        this.cache = EmailSyncSettingsCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.emailSyncManager = EmailSyncManager.getInstance();
    }

    private static final class Instance {
        private static final EmailSyncSettingResolverImpl INSTANCE = new EmailSyncSettingResolverImpl();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link EmailSyncSettingResolver}.
     * </p>
     *
     * @return the {@link EmailSyncSettingResolver} instance
     */
    public static EmailSyncSettingResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Resolves the {@link EmailSyncSettings} by orgId and syncId.
     * First tries cache, then loads from DB if absent.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param syncId Represents the sync configuration id
     * @return {@link Optional} of {@link EmailSyncSettings}
     */
    @Override
    public Optional<EmailSyncSettings> resolve(final long orgId, final long userId, final Long syncId) {
        if (Objects.isNull(syncId)) {
            return Optional.empty();
        }
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Optional<EmailSyncSettings> optionalEmailSyncSettings = cache.get(syncId);

            if (optionalEmailSyncSettings.isPresent()) {
                return optionalEmailSyncSettings;
            }
        } finally {
            readLock.unlock();
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            final EmailSyncSettings emailSyncSettings = emailSyncManager.get(orgId, userId, syncId);

            if (Objects.isNull(emailSyncSettings)) {
                return Optional.empty();
            }
            cache.put(syncId, emailSyncSettings);

            return Optional.of(emailSyncSettings);
        } finally {
            writeLock.unlock();
        }
    }

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
    @Override
    public Collection<EmailSyncSettings> resolveAll(final long orgId, final long userId, final Collection<Long> syncIds) {
        if (Objects.isNull(syncIds) || syncIds.isEmpty()) {
            return Collections.emptyList();
        }

        return syncIds.stream()
                .map(syncId -> resolve(orgId, userId, syncId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * Updates a collection of {@link EmailSyncSettings} via manager and refreshes the cache.
     * </p>
     *
     * @param orgId             Represents the organization id
     * @param emailSyncSettings the {@link Collection} of {@link EmailSyncSettings}
     */
    @Override
    public void update(final long orgId, final Collection<EmailSyncSettings> emailSyncSettings) {
        if (Objects.isNull(emailSyncSettings) || emailSyncSettings.isEmpty()) {
            return;
        }
        final Collection<EmailSyncSettings> settings = emailSyncSettings.stream()
                .filter(syncSettings -> Objects.nonNull(syncSettings) && Objects.nonNull(syncSettings.id()))
                .collect(Collectors.toList());

        if (settings.isEmpty()) {
            return;
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            emailSyncManager.update(orgId, settings);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * Removes the cached entry for the given sync id.
     * </p>
     *
     * @param syncId Represents the sync configuration id
     */
    @Override
    public void remove(final Long syncId) {
        if (Objects.isNull(syncId)) {
            return;
        }
        remove(Collections.singleton(syncId));
    }

    /**
     * <p>
     * Removes the cached entries for the given sync ids.
     * </p>
     *
     * @param syncIds Represents the sync configuration ids
     */
    @Override
    public void remove(final Collection<Long> syncIds) {
        if (Objects.isNull(syncIds) || syncIds.isEmpty()) {
            return;
        }
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            cache.remove(syncIds);
        } finally {
            writeLock.unlock();
        }
    }
}
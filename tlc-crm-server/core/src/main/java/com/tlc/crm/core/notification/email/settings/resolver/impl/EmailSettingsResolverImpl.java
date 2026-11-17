package com.tlc.crm.core.notification.email.settings.resolver.impl;

import com.tlc.commons.concurrent.LockStore;

import com.tlc.crm.core.notification.email.settings.api.EmailSettingsManager;
import com.tlc.crm.core.notification.email.settings.resolver.EmailSettingsResolver;
import com.tlc.crm.core.notification.email.settings.internal.cache.EmailSettingsCache;

import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the {@link EmailSettings} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see EmailSettingsResolver
 */
public class EmailSettingsResolverImpl implements EmailSettingsResolver {

    private static final byte STRIPES_COUNT = 10;

    private final EmailSettingsCache emailSettingsCache;
    private final EmailSettingsManager emailSettingsManager;
    private final LockStore<Long> lockStore;

    private EmailSettingsResolverImpl() {
        emailSettingsCache = EmailSettingsCache.getInstance();
        emailSettingsManager = EmailSettingsManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final EmailSettingsResolverImpl INSTANCE = new EmailSettingsResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link EmailSettingsResolverImpl} instance
     * </p>
     *
     * @return The {@link EmailSettingsResolverImpl}
     */
    public static EmailSettingsResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return An Optional containing the {@link EmailSettings}
     */
    @Override
    public Optional<EmailSettings> get(final long orgId, final long userId) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final Optional<EmailSettings> existingEmailSettings;

        readLock.lock();

        try {
            existingEmailSettings = emailSettingsCache.get(orgId, userId);
        } finally {
            readLock.unlock();
        }

        if (existingEmailSettings.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();

            try {
                final EmailSettings emailSettings = emailSettingsManager.retrieveEmailSettings(orgId, userId);

                if (Objects.nonNull(emailSettings)) {
                    emailSettingsCache.set(emailSettings);

                    return Optional.of(emailSettings);
                }
            } finally {
                writeLock.unlock();
            }
        }

        return existingEmailSettings;
    }
}

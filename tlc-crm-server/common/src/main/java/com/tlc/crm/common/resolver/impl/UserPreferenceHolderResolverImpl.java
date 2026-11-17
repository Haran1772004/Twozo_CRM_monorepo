package com.tlc.crm.common.resolver.impl;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.crm.common.cache.UserPreferenceHolderCache;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.resolver.UserPreferenceHolderResolver;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the preferences of the user.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class UserPreferenceHolderResolverImpl implements UserPreferenceHolderResolver {

    private static final byte STRIPES_COUNT = 10;

    private final UserPreferenceHolderCache userPreferenceHolderCache;
    private final UserPreferenceManager userPreferenceManager;
    private final LockStore<Long> lockStore;

    private UserPreferenceHolderResolverImpl() {
        userPreferenceHolderCache = UserPreferenceHolderCache.get();
        userPreferenceManager = UserPreferenceManager.getInstance();
        lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final UserPreferenceHolderResolverImpl INSTANCE = new UserPreferenceHolderResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderResolverImpl} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderResolverImpl}.
     */
    public static UserPreferenceHolderResolverImpl getInstance() {
        return UserPreferenceHolderResolverImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link UserPreferenceHolder}.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param userId A user's id.
     * @return {@link UserPreferenceHolder}.
     */
    @Override
    public UserPreferenceHolder get(final long orgId, final long userId) {
        final ReentrantReadWriteLock reentrantReadWriteLock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        final UserPreferenceHolder existingPreferenceHolder;

        readLock.lock();

        try {
            existingPreferenceHolder = userPreferenceHolderCache.get(orgId, userId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingPreferenceHolder)) {
            final ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

            writeLock.lock();
            final UserPreferenceHolder preferenceHolder;

            try {
                preferenceHolder = userPreferenceManager.get(orgId, userId);

                userPreferenceHolderCache.add(userId, preferenceHolder);
            } finally {
                writeLock.unlock();
            }
            return preferenceHolder;
        }
        return existingPreferenceHolder;
    }
}

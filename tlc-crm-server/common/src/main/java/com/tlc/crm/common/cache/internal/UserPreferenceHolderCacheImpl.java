package com.tlc.crm.common.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.common.cache.UserPreferenceHolderCache;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the preference of the user.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class UserPreferenceHolderCacheImpl implements UserPreferenceHolderCache {

    private final Cache<String, UserPreferenceHolder> preferenceHolderCache;

    private UserPreferenceHolderCacheImpl() {
        final CacheConfig<String, UserPreferenceHolder> preferenceCacheConfig = new CacheConfig<>("preferenceHolderCache");

        preferenceCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(10));
        preferenceHolderCache = CacheServiceProvider.get().createOrOpenCache(preferenceCacheConfig);
    }

    private static class Instance {
        private static final UserPreferenceHolderCacheImpl INSTANCE = new UserPreferenceHolderCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link UserPreferenceHolderCacheImpl} instance.
     * </p>
     *
     * @return {@link UserPreferenceHolderCacheImpl}.
     */
    public static UserPreferenceHolderCacheImpl getInstance() {
        return UserPreferenceHolderCacheImpl.Instance.INSTANCE;
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
        return preferenceHolderCache.get(buildKey(orgId, userId));
    }

    /**
     * Adds {@link UserPreferenceHolder}.
     *
     * @param userId               Represents a user's id.
     * @param userPreferenceHolder {@link UserPreferenceHolder}.
     */
    @Override
    public void add(final long userId, final UserPreferenceHolder userPreferenceHolder) {
        preferenceHolderCache.put(buildKey(userPreferenceHolder.orgId(), userId), userPreferenceHolder);
    }

    /**
     * <p>
     * Removes the user's preference from cache.
     * </p>
     *
     * @param orgId  Represents an organization's id.
     * @param userId Represents a user's id.
     */
    @Override
    public void remove(final long orgId, final Long userId) {
        preferenceHolderCache.remove(buildKey(orgId, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {

    }

    /**
     * <p>
     * Builds a unique key based on the provided organization id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return A unique key string combining the organization id and user id
     */
    private String buildKey(final long orgId, final long userId) {
        return String.format("%d%s%d", orgId, "_", userId);
    }
}

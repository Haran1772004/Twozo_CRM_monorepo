package com.tlc.crm.common.iam.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.cache.UserCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the details of {@link CrmUser} into cache
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserCacheImpl implements UserCache {

    private final Cache<Long, CrmUser> iamUserCache;
    private final Cache<String, CrmUser> crmUserCache;

    public UserCacheImpl() {
        final CacheConfig<Long, CrmUser> iamCacheConfig = new CacheConfig<>("crmUserCacheByIamId");
        final CacheConfig<String, CrmUser> cacheConfig = new CacheConfig<>("crmUserCache");
        final int expireTime = (int) TimeUnit.MINUTES.toMillis(10);

        iamCacheConfig.setTtl(expireTime);
        cacheConfig.setTtl(expireTime);
        iamUserCache = CacheServiceProvider.get().createOrOpenCache(iamCacheConfig);
        crmUserCache = CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    private static class Instance {
        private static final UserCacheImpl INSTANCE = new UserCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link UserCacheImpl} instance
     * </p>
     *
     * @return The {@link UserCacheImpl}
     */
    public static UserCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userIds Contains the collection of user ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds) {
        final Collection<CrmUser> users = new ArrayList<>();

        for (final Long userId : userIds) {
            final CrmUser crmUser = crmUserCache.get(buildCacheKey(orgId, userId));

            if (Objects.nonNull(crmUser)) {
                users.add(crmUser);
            }
        }

        return users;
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    @Override
    public void put(final CrmUser crmUser) {
        crmUserCache.put(buildCacheKey(crmUser.orgId(), crmUser.id()), crmUser);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamId Represents the iam user id
     * @return The crm user id
     */
    @Override
    public Long getUserId(final Long iamId) {
        final CrmUser crmUser = iamUserCache.get(iamId);

        return Objects.isNull(crmUser) ? null : crmUser.id();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    @Override
    public void putByIamId(final CrmUser crmUser) {
        iamUserCache.put(crmUser.getIamId(), crmUser);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} details
     */
    @Override
    public CrmUser getUser(final Long orgId, final Long userId) {
        return crmUserCache.get(buildCacheKey(orgId, userId));
    }

    /**
     * {@inheritDoc}
     *
     * @param iamUserIds Contains the collection of iam user ids
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsersByIamId(final Collection<Long> iamUserIds) {
        final Collection<CrmUser> crmUsers = new ArrayList<>();

        for (final Long userId : iamUserIds) {
            final CrmUser crmUser = iamUserCache.get(userId);

            if (Objects.nonNull(crmUser)) {
                crmUsers.add(crmUser);
            }
        }

        return crmUsers;
    }

    /**
     * <p>
     * Removes the user from the cache
     * </p>
     *
     * @param userId  Represents the user ID
     */
    @Override
    public void removeUser(final Long orgId, final Long userId) {
        crmUserCache.remove(buildCacheKey(orgId, userId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
    }

    /**
     * <p>
     * Builds the cache key by org id and user id
     * </p>
     *
     * @param orgId  Represents the org id
     * @param userId Represents the user id
     * @return the key
     */
    private String buildCacheKey(final Long orgId, final Long userId) {
        return StringUtil.join(StringUtil.Delimiter.COLON, String.valueOf(orgId), String.valueOf(userId));
    }
}

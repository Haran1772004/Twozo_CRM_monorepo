package com.tlc.crm.common.iam.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.cache.RoleCache;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the details of {@link CrmRole} into cache
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class RoleCacheImpl implements RoleCache {

    private final Cache<Long, CrmRole> roleCache;

    private RoleCacheImpl() {
        final CacheConfig<Long, CrmRole> cacheConfig = new CacheConfig<>();
        final long expireTime = TimeUnit.SECONDS.toMillis(30);

        cacheConfig.setExpireAfterAccess(expireTime);
        roleCache = CacheManager.getInstance().createCache(cacheConfig);
    }

    private static class Instance {
        private static final RoleCacheImpl INSTANCE = new RoleCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link RoleCacheImpl} instance
     * </p>
     *
     * @return The {@link RoleCacheImpl}
     */
    public static RoleCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole getRoleByUser(final Long userId) {
        return roleCache.get(userId);
    }

    /**
     * {@inheritDoc}
     *
     * @param userId  Represents the user id
     * @param crmRole Represents the {@link CrmRole} details
     */
    @Override
    public void loadRoleIntoCache(final Long userId, final CrmRole crmRole) {
        roleCache.put(userId, crmRole);
    }

    /**
     * {@inheritDoc}
     *
     * @param userId  Represents the user id
     */
    @Override
    public void remove(final long userId) {
        roleCache.remove(userId);
    }
}

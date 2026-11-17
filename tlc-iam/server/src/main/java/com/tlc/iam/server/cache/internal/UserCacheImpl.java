package com.tlc.iam.server.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.iam.common.models.User;
import com.tlc.iam.server.cache.UserCache;
import com.tlc.cache.remote.CacheConfig;

import java.util.concurrent.TimeUnit;

/**
 * Provides the implementation for {@link UserCache}
 *
 * @author Maharaja S
 * @version 1.0
 */
public class UserCacheImpl implements UserCache {

    private static final String IAM_USER_CACHE = "iamUserCache";

    private final Cache<Long, User> userCache;

    private UserCacheImpl() {
        final CacheConfig<Long, User> userCacheConfig = new CacheConfig<>(IAM_USER_CACHE);

        userCacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(2));

        userCache = CacheServiceProvider.get().createOrOpenCache(userCacheConfig);
    }

    private static final class Instance {
        private static final UserCacheImpl USER_CACHE = new UserCacheImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link  UserCacheImpl}
     * </p>
     *
     * @return the instance of {@link UserCacheImpl}
     */
    public static UserCacheImpl getInstance() {
        return Instance.USER_CACHE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamUserId Represents the iam user id
     * @return
     */
    @Override
    public User get(final Long iamUserId) {
        return userCache.get(iamUserId);
    }

    /**
     * {@inheritDoc}
     *
     * @param user Represents the {@link User}
     */
    @Override
    public void add(final User user) {
        userCache.put(user.id(), user);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamUserId Represents the iam user id
     */
    @Override
    public void remove(final Long iamUserId) {
        userCache.remove(iamUserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        userCache.clear();
    }
}
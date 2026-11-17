package com.tlc.iam.server.cache;

import com.tlc.iam.common.models.User;
import com.tlc.iam.server.cache.internal.UserCacheImpl;

/**
 * Stores the {@link User} into cache
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface UserCache {

    /**
     * <p>
     * Gets the instance of {@link  UserCacheImpl}
     * </p>
     *
     * @return the instance of {@link UserCacheImpl}
     */
    static UserCacheImpl getInstance() {
        return UserCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the user from the cache by the specified organization id and user id
     * </p>
     *
     * @param iamUserId Represents the iam user id
     * @return the instance of {@link User}
     */
    User get(final Long iamUserId);

    /**
     * <p>
     * Adds the user to the cache
     * </p>
     *
     * @param user Represents the {@link User}
     */
    void add(final User user);

    /**
     * <p>
     * Removes the user from the cache
     * </p>
     *
     * @param iamUserId Represents the iam user id
     */
    void remove(final Long iamUserId);

    /**
     * <p>
     * Clears the cache
     * </p>
     */
    void clear();
}

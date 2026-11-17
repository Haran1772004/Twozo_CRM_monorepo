package com.tlc.crm.common.iam.cache;

import com.tlc.crm.common.cache.RemoteCache;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.cache.internal.UserCacheImpl;

import java.util.Collection;

/**
 * <p>
 * Provides the services for managing the {@link UserCache}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface UserCache extends RemoteCache {

    /**
     * <p>
     * Gets the {@link UserCache} instance
     * </p>
     *
     * @return {@link UserCacheImpl}
     */
    static UserCache getInstance() {
        return UserCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the collection of {@link CrmUser} by org id and user ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Represents the collection of user ids
     * @return The Collection of {@link CrmUser}
     */
    Collection<CrmUser> getUsers(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Loads the {@link CrmUser} into {@link UserCache}
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    void put(final CrmUser crmUser);

    /**
     * <p>
     * Gets the crm user id by iam id
     * </p>
     *
     * @param iamId Represents the iam user id
     * @return The crm user id
     */
    Long getUserId(final Long iamId);

    /**
     * <p>
     * Loads the {@link CrmUser} into {@link UserCache} by iam id
     * </p>
     *
     * @param crmUser Represents the {@link CrmUser} details
     */
    void putByIamId(final CrmUser crmUser);

    /**
     * <p>
     * Gets the {@link CrmUser} by org id and user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmUser} details
     */
    CrmUser getUser(final Long orgId, final Long userId);

    /**
     * <p>
     * Gets the collection of {@link CrmUser} by iam user ids
     * </p>
     *
     * @param iamUserIds Contains the collection of iam user ids
     * @return The collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsersByIamId(final Collection<Long> iamUserIds);

    /**
     * <p>
     * Removes the user from the cache
     * </p>
     *
     * @param orgId   Represents the org id
     * @param userId  Represents the user ID
     */
    void removeUser(final Long orgId, final Long userId);
}

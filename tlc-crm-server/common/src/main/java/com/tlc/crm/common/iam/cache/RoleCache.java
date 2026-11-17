package com.tlc.crm.common.iam.cache;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.cache.internal.RoleCacheImpl;

/**
 * <p>
 * Provides the services for managing the {@link RoleCache}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface RoleCache {

    /**
     * <p>
     * Gets the {@link RoleCache} instance
     * </p>
     *
     * @return The {@link RoleCacheImpl} instance
     */
    static RoleCache getInstance() {
        return RoleCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CrmRole} details by user id
     * </p>
     *
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    CrmRole getRoleByUser(final Long userId);

    /**
     * <p>
     * Loads the {@link CrmRole} into {@link RoleCache}
     * </p>
     *
     * @param userId  Represents the user id
     * @param crmRole Represents the {@link CrmRole} details
     */
    void loadRoleIntoCache(final Long userId, final CrmRole crmRole);

    /**
     * <p>
     * Removes the {@link CrmRole} from the {@link RoleCache}
     * </p>
     *
     * @param userId  Represents the user id
     */
    void remove(final long userId);
}

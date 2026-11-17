package com.tlc.crm.security.internal.permission.service.cache;

import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Offers services for retrieving the {@link RolePermissions}(s) associated with roles.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RolePermissionCache extends RemoteCache {

    /**
     * Gets the instance of the {@link RolePermissionCache}
     *
     * @return {@link RolePermissionCache}
     */
    static RolePermissionCache getInstance() {
        return RolePermissionCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link RolePermission} based on the specified organization, role.
     * </p>
     *
     * @param orgId  Represents the organization id.
     * @param roleId Represents the role id.
     * @return Collection of {@link RolePermission} that holds the permissions of the actions
     */
    Collection<RolePermission> getRolePermissions(final long orgId, final long roleId);

    /**
     * <p>
     * Fetches the collection of permissions based on the specified organization, role, and subcategory.
     * If the permissions are not already cached, they will be fetched from the database, loaded into the cache, and then returned.
     * </p>
     *
     * @param orgId   Represents the organization id.
     * @param roleIds Represents the role id.
     * @return Collection of {@link RolePermissions} that holds the permissions of the roles
     */
    Set<RolePermissions> getRolePermissions(final long orgId, final Collection<Long> roleIds);

    /**
     * <p>
     * Adds the {@link RolePermissions} into cache.
     * </p>
     *
     * @param rolePermissions {@link RolePermissions} holds the permissions of the role.
     */
    void addPermission(final RolePermissions rolePermissions);

    /**
     * <p>
     * Invalidates the role permission cache based on action configs.
     * </p>
     *
     * @param orgId   Represents the org id.
     * @param roleIds Represents the role id.
     */
    void invalidateCache(final long orgId, final Set<Long> roleIds);
}

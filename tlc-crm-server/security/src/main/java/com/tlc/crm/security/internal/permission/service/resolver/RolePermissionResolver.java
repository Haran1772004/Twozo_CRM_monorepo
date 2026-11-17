package com.tlc.crm.security.internal.permission.service.resolver;

import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;

import java.util.Collection;
import java.util.Set;

/**
 * Resolver for retrieving and handling essential data related to permissions.
 *
 * @author Sathish
 * @version 1.0
 */
public interface RolePermissionResolver {

    /**
     * Gets the instance of {@link RolePermissionResolver}
     *
     * @return {@link RolePermissionResolver}
     */
    static RolePermissionResolver getInstance() {
        return RolePermissionResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the collection of actions based on the specified organization, role, and subcategory.
     * If the actions are not already cached, they will be fetched from the database, loaded into the cache, and then returned.
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
     * @return Collection of {@link RolePermissions} that holds the permissions of the actions
     */
    Collection<RolePermissions> getRolePermissions(final long orgId, final Set<Long> roleIds);
}

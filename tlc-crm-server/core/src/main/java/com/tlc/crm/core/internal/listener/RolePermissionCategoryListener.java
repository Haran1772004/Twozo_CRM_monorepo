package com.tlc.crm.core.internal.listener;

import com.tlc.crm.security.permission.model.RolePermission;

import java.util.Collection;

/**
 * <p>
 * Listener for handling notifications when role permissions in a specific category are added, modified, or made available.
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public interface RolePermissionCategoryListener {

    /**
     * <p>
     * Listens when a set of role permissions is provided
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param roleId          Represents the role id
     * @param rolePermissions Which contains the collections of {@link RolePermission}
     */
    void listen(final long orgId, final long roleId, final Collection<RolePermission> rolePermissions);
}

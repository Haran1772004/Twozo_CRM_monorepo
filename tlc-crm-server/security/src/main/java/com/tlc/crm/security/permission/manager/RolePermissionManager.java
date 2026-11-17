package com.tlc.crm.security.permission.manager;

import com.tlc.crm.security.internal.permission.manager.RolePermissionManagerImpl;
import com.tlc.crm.security.permission.model.RolePermission;
import com.tlc.crm.security.permission.model.RolePermissions;

/**
 * <p>
 * Provides the {@link RolePermission} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RolePermissionManager extends PermissionManager<RolePermission, RolePermissions> {

    static RolePermissionManager getInstance() {
        return RolePermissionManagerImpl.getInstance();
    }
}
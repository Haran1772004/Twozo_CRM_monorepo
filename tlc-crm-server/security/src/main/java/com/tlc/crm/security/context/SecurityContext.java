package com.tlc.crm.security.context;

import com.tlc.commons.executor.context.Context;

import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.internal.context.PermissionSecurityContext;
import com.tlc.crm.security.internal.context.SecurityContextHolder;
import com.tlc.crm.security.permission.model.RolePermission;

/**
 * Represents the security context to hold the {@link BaseCrmUser} and user principal
 *
 * @author sundar
 * @version 1.0
 */
public interface SecurityContext extends Context {

    /**
     * Loads the security context into the holder
     *
     * @param user to be loaded into the security context
     */
    static void load(final BaseCrmUser user) {

        if (!exists()) {
            SecurityContextHolder.getInstance().setContext(new PermissionSecurityContext(user));
        }
    }

    /**
     * Checks if the security context exists.
     *
     * @return true if the security context exists, false otherwise
     */
    static boolean exists() {
        return SecurityContextHolder.getInstance().exists();
    }

    /**
     * Clears the security context value from the current thread
     */
    static void clear() {
        SecurityContextHolder.getInstance().clear();
    }

    /**
     * Gets the unique identifier for the context
     *
     * @return the unique ID of the context
     */
    String id();

    /**
     * Gets the {@link BaseCrmUser}
     *
     * @return {@link BaseCrmUser}
     */
    <T extends BaseCrmUser> T getUser();

    /**
     * Gets the {@link RolePermission}
     *
     * @return {@link RolePermission}
     */
    RolePermission getRolePermission();
}
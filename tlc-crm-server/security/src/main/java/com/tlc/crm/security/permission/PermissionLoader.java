package com.tlc.crm.security.permission;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.security.internal.context.PermissionSecurityContext;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.internal.context.SecurityContextHolder;
import com.tlc.crm.security.internal.status.SecurityErrorCodes;
import com.tlc.crm.security.permission.model.RolePermission;

import java.util.Objects;

/**
 * Loader to load the permission into the current {@link SecurityContext}
 *
 * @author sundar
 * @version 1.0
 */
public final class PermissionLoader {

    private final SecurityContextHolder securityContextHolder;

    private PermissionLoader() {
        this.securityContextHolder = SecurityContextHolder.getInstance();
    }

    private static class Instance {
        private static final PermissionLoader PERMISSION_LOADER = new PermissionLoader();
    }

    /**
     * Gets the {@link PermissionLoader} instance
     *
     * @return {@link PermissionLoader}
     */
    public static PermissionLoader getInstance() {
        return Instance.PERMISSION_LOADER;
    }

    /**
     * Loads the {@link RolePermission} into the {@link SecurityContext}
     *
     * @param permission Represents the permission to be set
     */
    public void loadPermissionIntoContext(final RolePermission permission) {

        if (!SecurityContext.exists()) {
            throw ErrorCode.get(SecurityErrorCodes.SECURITY_CONTEXT_NOT_FOUND);
        }

        securityContextHolder.getContext().ifPresent(securityContext -> {

            if (Objects.isNull(securityContext.getRolePermission())) {
                ((PermissionSecurityContext) securityContext).setPermission(permission);
            }
        });
    }
}
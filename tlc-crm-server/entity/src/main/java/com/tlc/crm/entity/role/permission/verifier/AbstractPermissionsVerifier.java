package com.tlc.crm.entity.role.permission.verifier;

import com.tlc.crm.entity.role.permission.handler.CommonApiPermissionHandler;

import com.tlc.crm.security.permission.service.RolePermissionService;
import com.tlc.crm.security.permission.verifier.PermissionsVerifier;

import java.lang.annotation.Annotation;

/**
 * <p>
 * Verifies the permissions of the permission categories.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractPermissionsVerifier<T extends Annotation> implements PermissionsVerifier<T> {

    protected final RolePermissionService rolePermissionService;
    protected final CommonApiPermissionHandler commonApiPermissionHandler;

    protected AbstractPermissionsVerifier() {
        rolePermissionService = RolePermissionService.getInstance();
        commonApiPermissionHandler = CommonApiPermissionHandler.getInstance();
    }
}

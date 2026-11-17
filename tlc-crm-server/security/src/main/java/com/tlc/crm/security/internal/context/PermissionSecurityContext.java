package com.tlc.crm.security.internal.context;

import com.tlc.commons.executor.context.Context;
import com.tlc.commons.util.RandomIdGenerator;

import com.tlc.crm.security.context.CrmContextType;
import com.tlc.crm.security.context.SecurityContext;
import com.tlc.crm.security.iam.BaseCrmUser;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;
import com.tlc.crm.security.internal.permission.handler.DataPermissionHandlerImpl;
import com.tlc.crm.security.internal.permission.registry.DataPermissionHandlerRegistry;

import com.tlc.crm.security.permission.model.RolePermission;

import java.time.Instant;
import java.util.Objects;

/**
 * Responsible for containing details about the current user and their permissions
 *
 * @author sundar
 * @version 1.0
 */
public class PermissionSecurityContext implements SecurityContext {

    private final BaseCrmUser user;
    private RolePermission rolePermission;
    private final String id;

    public PermissionSecurityContext(final BaseCrmUser user) {
        this(user, null, null);
    }

    public PermissionSecurityContext(final BaseCrmUser user, final String contextId, final RolePermission rolePermission) {
        this.user = user;
        this.rolePermission = rolePermission;
        this.id = Objects.isNull(contextId) ? RandomIdGenerator.getInstance().generateUniqueId(user.id(), Instant.now().toString()) : contextId;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Type} that represents the type of the context
     */
    @Override
    public Context.Type getType() {
        return CrmContextType.SECURITY;
    }

    /**
     * {@inheritDoc}
     *
     * @return a new instance of the context, identical to the original
     */
    @Override
    public Context copy() {
        return new PermissionSecurityContext(user, id, rolePermission);
    }

    /**
     * {@inheritDoc}
     *
     * @return the unique ID of the context
     */
    @Override
    public String id() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link BaseCrmUser}
     */
    @Override
    public <T extends BaseCrmUser> T getUser() {
        return (T) user;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link RolePermission}
     */
    @Override
    public RolePermission getRolePermission() {
        return rolePermission;
    }

    /**
     * Sets the permission into the context
     *
     * @param rolePermission Represents the role permission to be set
     */
    public void setPermission(final RolePermission rolePermission) {
        this.rolePermission = rolePermission;

        if (Objects.nonNull(rolePermission)) {
            final DataPermissionHandlerRegistry dataPermissionHandlerRegistry = DataPermissionHandlerRegistry.getInstance();

            if (dataPermissionHandlerRegistry.contains()) {
                final DataPermissionHandler dataPermissionHandler = dataPermissionHandlerRegistry.get();

                if (Objects.isNull(dataPermissionHandler.getRolePermission())) {
                    ((DataPermissionHandlerImpl) dataPermissionHandler).setRolePermission(rolePermission);
                }
            }
        }
    }
}
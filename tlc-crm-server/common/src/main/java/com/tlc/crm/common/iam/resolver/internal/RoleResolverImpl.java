package com.tlc.crm.common.iam.resolver.internal;

import com.tlc.crm.common.iam.api.manager.RoleManager;
import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.cache.RoleCache;
import com.tlc.crm.common.iam.resolver.RoleResolver;

import java.util.Objects;

/**
 * <p>
 * A resolver that resolves the {@link CrmRole} related details
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class RoleResolverImpl implements RoleResolver {

    private final RoleCache roleCache;
    private final RoleManager roleManager;

    private RoleResolverImpl() {
        roleCache = RoleCache.getInstance();
        roleManager = RoleManager.getInstance();
    }

    private static class Instance {
        private static final RoleResolverImpl INSTANCE = new RoleResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link RoleResolver} instance
     * </p>
     *
     * @return The {@link RoleResolverImpl} instance
     */
    public static RoleResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    @Override
    public CrmRole getRoleByUser(final Long orgId, final Long userId) {
        final CrmRole existingRole = roleCache.getRoleByUser(userId);

        if (Objects.isNull(existingRole)) {
            final CrmRole crmRole = roleManager.getRoleByUser(orgId, userId);

            roleCache.loadRoleIntoCache(userId, crmRole);

            return crmRole;
        }

        return existingRole;
    }
}

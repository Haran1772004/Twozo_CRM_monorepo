package com.tlc.crm.common.iam.service.internal;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.resolver.RoleResolver;
import com.tlc.crm.common.iam.service.RoleService;

/**
 * <p>
 * It provides the implementation for various operations related to the {@link RoleService}
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class RoleServiceImpl implements RoleService {

    private final RoleResolver roleResolver;

    private RoleServiceImpl() {
        roleResolver = RoleResolver.getInstance();
    }

    private static class Instance {
        private static final RoleServiceImpl INSTANCE = new RoleServiceImpl();
    }

    /**
     * <p>
     * Gets the role service instance
     * </p>
     *
     * @return The {@link RoleServiceImpl} instance
     */
    public static RoleServiceImpl getInstance() {
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
        return roleResolver.getRoleByUser(orgId, userId);
    }
}

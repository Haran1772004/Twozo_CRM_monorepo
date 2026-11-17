package com.tlc.crm.common.iam.resolver;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.resolver.internal.RoleResolverImpl;

/**
 * <p>
 * A resolver that resolves the {@link CrmRole} related details
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface RoleResolver {

    /**
     * <p>
     * Gets the {@link RoleResolver} instance
     * </p>
     *
     * @return The {@link RoleResolverImpl} instance
     */
    static RoleResolver getInstance() {
        return RoleResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CrmRole} details by user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return The {@link CrmRole} details
     */
    CrmRole getRoleByUser(final Long orgId, final Long userId);
}

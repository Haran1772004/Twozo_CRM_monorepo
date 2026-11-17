package com.tlc.crm.common.iam.service;

import com.tlc.crm.common.iam.api.model.CrmRole;
import com.tlc.crm.common.iam.service.internal.RoleServiceImpl;

/**
 * <p>
 * The {@link RoleService} provides the service for the {@link CrmRole}
 * </P>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface RoleService {

    /**
     * <p>
     * Gets the {@link RoleService} instance
     * </p>
     *
     * @return The {@link RoleServiceImpl} instance
     */
    static RoleService getInstance() {
        return RoleServiceImpl.getInstance();
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

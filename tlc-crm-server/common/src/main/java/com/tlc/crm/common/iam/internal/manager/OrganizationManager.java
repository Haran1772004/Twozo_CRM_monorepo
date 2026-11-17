package com.tlc.crm.common.iam.internal.manager;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.internal.manager.impl.OrganizationManagerImpl;

import java.util.Collection;

/**
 * <p>
 * Performs the functionality for the {@link OrganizationManager} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface OrganizationManager {

    /**
     * <p>
     * Gets the instance of {@link OrganizationManager}
     * </p>
     *
     * @return {@link OrganizationManager}
     */
    static OrganizationManager getInstance() {
        return OrganizationManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the org initialization status given by {@link CrmOrg} and {@link CrmOrg.InitializationStatus}
     * </p>
     *
     * @param iamOrgId Represents the iam org id
     * @param status   Represents the org initialization status
     */
    void updateInitializationStatus(final Long iamOrgId, final CrmOrg.InitializationStatus status);

    /**
     * <p>
     * Gets the {@link CrmOrg} by iam org id
     * </p>
     *
     * @param iamOrgId Represents the iam org id
     * @return The {@link CrmOrg} details
     */
    CrmOrg getOrgByIamId(final Long iamOrgId);

    /**
     * <p>
     * Gets the {@link CrmOrg} by org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The {@link CrmOrg} details
     */
    CrmOrg get(final Long orgId);

    /**
     * <p>
     * Gets all {@link CrmOrg} records
     * </p>
     *
     * @return A collection of {@link CrmOrg} details
     */
    Collection<CrmOrg> getAll();
}
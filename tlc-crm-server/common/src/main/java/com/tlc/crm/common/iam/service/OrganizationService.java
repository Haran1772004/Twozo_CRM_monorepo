package com.tlc.crm.common.iam.service;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.service.internal.OrganizationServiceImpl;

import java.util.Collection;

/**
 * <p>
 * The {@link OrganizationService} provides the service for the organization
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface OrganizationService {

    /**
     * <p>
     * Gets the instance of the {@link OrganizationService}
     * </p>
     *
     * @return The {@link OrganizationServiceImpl} instance
     */
    static OrganizationService getInstance() {
        return OrganizationServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the org id based on the provided IAM org Id
     * </p>
     *
     * @param iamOrgId Represents an IAM org id
     * @return The crm org id
     */
    Long getOrgId(final Long iamOrgId);

    /**
     * <p>
     * Fetches the iam org id based on the provided crm org Id
     * </p>
     *
     * @param orgId Represents an org id
     * @return The iam org id
     */
    Long getIamOrgId(final Long orgId);

    /**
     * Fetches the {@link CrmOrg} based on the provided organization ID
     *
     * @param orgId Represents the CRM organization ID
     * @return The {@link CrmOrg} object containing details about the CRM Organization
     */
    CrmOrg getCrmOrg(final Long orgId);

    /**
     * <p>
     * Gets all {@link CrmOrg} records
     * </p>
     *
     * @return A collection of {@link CrmOrg} details
     */
    Collection<CrmOrg> getAll();
}

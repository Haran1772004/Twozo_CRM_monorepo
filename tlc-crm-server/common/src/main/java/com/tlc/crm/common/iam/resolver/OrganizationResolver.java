package com.tlc.crm.common.iam.resolver;

import com.tlc.crm.common.iam.resolver.internal.OrganizationResolverImpl;
import com.tlc.crm.common.iam.api.model.CrmOrg;

import java.util.Collection;

/**
 * <p>
 * A resolver that resolves the {@link CrmOrg} related details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface OrganizationResolver {

    /**
     * <p>
     * Gets the instance of {@link OrganizationResolver}
     * </p>
     *
     * @return The instance of {@link OrganizationResolverImpl}
     */
    static OrganizationResolver getInstance() {
        return OrganizationResolverImpl.getInstance();
    }

    /**
     * <p>
     * Gets the crm org id
     * </p>
     *
     * @param orgId Represents an iam org id
     * @return The crm org id
     */
    Long getOrgId(final Long orgId);

    /**
     * <p>
     * Gets the iam org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The iam org id
     */
    Long getIamOrgId(final Long orgId);

    /**
     * <p>
     * Fetches the {@link CrmOrg} associated with the provided organization ID from the cache.
     * If the {@link CrmOrg} is not found in the cache, it will be fetched from the database and stored in the cache before being returned
     * </p>
     *
     * @param orgId Represents the CRM organization ID
     * @return The {@link CrmOrg} associated with the specified organization
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

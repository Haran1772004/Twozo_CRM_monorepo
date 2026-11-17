package com.tlc.crm.common.iam.resolver.internal;

import com.tlc.crm.common.iam.internal.manager.OrganizationManager;
import com.tlc.crm.common.iam.cache.OrganizationCache;
import com.tlc.crm.common.iam.resolver.OrganizationResolver;
import com.tlc.crm.common.iam.api.model.CrmOrg;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A resolver that resolves the {@link CrmOrg} related details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class OrganizationResolverImpl implements OrganizationResolver {

    private final OrganizationCache organizationCache;
    private final OrganizationManager organizationManager;

    public OrganizationResolverImpl() {
        organizationCache = OrganizationCache.getInstance();
        organizationManager = OrganizationManager.getInstance();
    }

    private static class Instance {
        private static final OrganizationResolverImpl INSTANCE = new OrganizationResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link OrganizationResolverImpl} instance
     * </p>
     *
     * @return The instance of {@link OrganizationResolverImpl}
     */
    public static OrganizationResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId Represents an iam org id
     * @return The crm org id
     */
    @Override
    public Long getOrgId(final Long iamOrgId) {
        final CrmOrg exitingCrmOrg = organizationCache.getOrgByIamOrg(iamOrgId);

        if (Objects.isNull(exitingCrmOrg)) {
            final CrmOrg crmOrg = organizationManager.getOrgByIamId(iamOrgId);

            organizationCache.storeIamOrgIntoCache(iamOrgId, crmOrg);

            return crmOrg.id();
        }

        return exitingCrmOrg.id();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @return The iam org id
     */
    @Override
    public Long getIamOrgId(final Long orgId) {
        final CrmOrg exitingIamOrg = organizationCache.getOrg(orgId);

        if (Objects.isNull(exitingIamOrg)) {
            final CrmOrg crmOrg = organizationManager.get(orgId);

            organizationCache.storeCrmOrgIntoCache(orgId, crmOrg);

            return crmOrg.getIamOrgId();
        }

        return exitingIamOrg.getIamOrgId();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the CRM organization ID
     * @return The {@link CrmOrg} associated with the specified organization
     */
    @Override
    public CrmOrg getCrmOrg(final Long orgId) {
        final CrmOrg existingCrmOrg = organizationCache.getOrg(orgId);

        if (Objects.isNull(existingCrmOrg)) {
            final CrmOrg crmOrg = organizationManager.get(orgId);

            organizationCache.storeCrmOrgIntoCache(orgId, crmOrg);

            return crmOrg;
        }
        return existingCrmOrg;
    }

    /**
     * <p>
     * Gets all {@link CrmOrg} records
     * </p>
     *
     * @return A collection of {@link CrmOrg} details
     */
    @Override
    public Collection<CrmOrg> getAll() {
        return organizationManager.getAll();
    }
}

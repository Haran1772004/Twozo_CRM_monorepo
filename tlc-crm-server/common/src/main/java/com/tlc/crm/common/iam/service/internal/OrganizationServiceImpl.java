package com.tlc.crm.common.iam.service.internal;

import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.resolver.OrganizationResolver;
import com.tlc.crm.common.iam.service.OrganizationService;

import java.util.Collection;

/**
 * <p>.
 * It provides the implementation for various operations related to the {@link OrganizationService}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationResolver organizationResolver;

    public OrganizationServiceImpl() {
        this.organizationResolver = OrganizationResolver.getInstance();
    }

    private static class Instance {
        private static final OrganizationServiceImpl INSTANCE = new OrganizationServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link OrganizationServiceImpl} instance
     * </p>
     *
     * @return The {@link OrganizationServiceImpl}
     */
    public static OrganizationServiceImpl getInstance() {
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
        return organizationResolver.getOrgId(iamOrgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an org id
     * @return The iam org id
     */
    @Override
    public Long getIamOrgId(final Long orgId) {
        return organizationResolver.getIamOrgId(orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the CRM organization ID
     * @return The {@link CrmOrg} object containing details about the CRM Organization
     */
    @Override
    public CrmOrg getCrmOrg(final Long orgId) {
        return organizationResolver.getCrmOrg(orgId);
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
        return organizationResolver.getAll();
    }
}

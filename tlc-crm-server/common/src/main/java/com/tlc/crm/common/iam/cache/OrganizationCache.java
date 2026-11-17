package com.tlc.crm.common.iam.cache;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.cache.internal.OrganizationCacheImpl;

/**
 * <p>
 * Provides the services for managing the {@link OrganizationCache}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface OrganizationCache extends LocalCache {

    /**
     * <p>
     * Gets the {@link OrganizationCache} instance
     * </P>
     *
     * @return {@link OrganizationCacheImpl}
     */
    static OrganizationCache getInstance() {
        return OrganizationCacheImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link CrmOrg} by iam org id
     * </p>
     *
     * @param iamOrgId Represents the iam org id
     * @return The {@link CrmOrg}
     */
    CrmOrg getOrgByIamOrg(final Long iamOrgId);

    /**
     * <p>
     * Loads the iam org and {@link CrmOrg} into cache
     * </p>
     *
     * @param iamOrgId Represents the iam org id
     * @param crmOrg   Represents the {@link CrmOrg} details
     */
    void storeIamOrgIntoCache(final Long iamOrgId, final CrmOrg crmOrg);

    /**
     * <p>
     * Loads the org and {@link CrmOrg} into cache
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param crmOrg Represents the {@link CrmOrg} details
     */
    void storeCrmOrgIntoCache(final Long orgId, final CrmOrg crmOrg);

    /**
     * <p>
     * Gets the {@link CrmOrg} by org id
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The {@link CrmOrg} details
     */
    CrmOrg getOrg(final Long orgId);
}

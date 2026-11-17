package com.tlc.crm.common.iam.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.crm.common.iam.cache.OrganizationCache;
import com.tlc.crm.common.iam.api.model.CrmOrg;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the details of {@link CrmOrg} into cache
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class OrganizationCacheImpl implements OrganizationCache {

    private final Cache<Long, CrmOrg> orgCache;
    private final Cache<Long, CrmOrg> iamOrgCache;

    public OrganizationCacheImpl() {
        final CacheManager cacheManager = CacheManager.getInstance();
        final CacheConfig<Long, CrmOrg> cacheConfig = new CacheConfig<>();
        final CacheConfig<Long, CrmOrg> iamCacheConfig = new CacheConfig<>();
        final long expireTime = TimeUnit.SECONDS.toMillis(5);

        cacheConfig.setExpireAfterAccess(expireTime);
        iamCacheConfig.setExpireAfterAccess(expireTime);
        orgCache = cacheManager.createCache(cacheConfig);
        iamOrgCache = cacheManager.createCache(iamCacheConfig);
    }

    private static class Instance {
        private static final OrganizationCacheImpl INSTANCE = new OrganizationCacheImpl();
    }

    /**
     * <p>
     * Gets the {@link OrganizationCacheImpl} instance
     * </p>
     *
     * @return The {@link OrganizationCacheImpl}
     */
    public static OrganizationCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId Represents the iam org id
     * @return The {@link CrmOrg} details
     */
    @Override
    public CrmOrg getOrgByIamOrg(final Long iamOrgId) {
        return iamOrgCache.get(iamOrgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId Represents the iam org id
     * @param crmOrg   Represents the {@link CrmOrg} details
     */
    @Override
    public void storeIamOrgIntoCache(final Long iamOrgId, final CrmOrg crmOrg) {
        iamOrgCache.put(iamOrgId, crmOrg);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organization id
     * @param crmOrg Represents the {@link CrmOrg} details
     */
    @Override
    public void storeCrmOrgIntoCache(final Long orgId, final CrmOrg crmOrg) {
        orgCache.put(orgId, crmOrg);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id
     * @return The {@link CrmOrg} details
     */
    @Override
    public CrmOrg getOrg(final Long orgId) {
        return orgCache.get(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
    }
}

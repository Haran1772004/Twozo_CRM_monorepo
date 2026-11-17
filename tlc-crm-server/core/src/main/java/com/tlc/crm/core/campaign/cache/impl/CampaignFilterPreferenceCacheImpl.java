package com.tlc.crm.core.campaign.cache.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.core.campaign.cache.CampaignFilterPreferenceCache;
import com.tlc.crm.core.campaign.model.CampaignFilterUserPreference;
import com.tlc.crm.core.campaign.model.CampaignCategory;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores campaign filter preferences in remote cache.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class CampaignFilterPreferenceCacheImpl implements CampaignFilterPreferenceCache {

    private final Cache<String, CampaignFilterUserPreference> preferenceCache;

    private static class InstanceHolder {
        private static final CampaignFilterPreferenceCacheImpl INSTANCE = new CampaignFilterPreferenceCacheImpl();
    }

    private CampaignFilterPreferenceCacheImpl() {
        final CacheConfig<String, CampaignFilterUserPreference> cacheConfig = new CacheConfig<>(CampaignFields.CAMPAIGN_FILTER_PREFERENCES);

        cacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(10));
        preferenceCache = CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    /**
     * <p>
     * Gets the instance of {@link CampaignFilterPreferenceCacheImpl}.
     * </p>
     *
     * @return {@link CampaignFilterPreferenceCacheImpl}.
     */
    public static CampaignFilterPreferenceCacheImpl getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return {@link CampaignFilterUserPreference}.
     */
    @Override
    public CampaignFilterUserPreference get(final Long orgId, final Long userId, final CampaignCategory category) {
        return preferenceCache.get(buildKey(orgId, userId, category));
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Represents the filter preference.
     */
    @Override
    public void put(final CampaignFilterUserPreference preference) {
        preferenceCache.put(buildKey(preference.orgId(), preference.getUserId(), preference.getCategory()), preference);
    }

    /**
     * <p>
     * Builds the cache key for the given org, user and category.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param userId   Represents the user id.
     * @param category Represents the campaign category.
     * @return Cache key.
     */
    private String buildKey(final Long orgId, final Long userId, final CampaignCategory category) {
        return String.format("%d:%d:%d", orgId, userId, category.getId());
    }
}
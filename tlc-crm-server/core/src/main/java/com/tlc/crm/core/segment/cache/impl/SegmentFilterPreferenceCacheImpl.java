package com.tlc.crm.core.segment.cache.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.core.segment.cache.SegmentFilterPreferenceCache;
import com.tlc.crm.core.segment.models.SegmentFilterUserPreference;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores segment filter preferences in remote cache.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentFilterPreferenceCacheImpl implements SegmentFilterPreferenceCache {

    private final Cache<Long, Map<Long, SegmentFilterUserPreference>> preferenceCache;

    private static class Instance {
        private static final SegmentFilterPreferenceCacheImpl INSTANCE = new SegmentFilterPreferenceCacheImpl();
    }

    private SegmentFilterPreferenceCacheImpl() {
        final CacheConfig<Long, Map<Long, SegmentFilterUserPreference>> cacheConfig = new CacheConfig<>("segmentFilterPreferences");

        cacheConfig.setTtl((int) TimeUnit.MINUTES.toMillis(1));

        preferenceCache = CacheServiceProvider.get().createOrOpenCache(cacheConfig);
    }

    /**
     * <p>
     * Gets the instance of {@link SegmentFilterPreferenceCacheImpl}.
     * </p>
     *
     * @return {@link SegmentFilterPreferenceCacheImpl}.
     */
    public static SegmentFilterPreferenceCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SegmentFilterUserPreference get(final Long orgId, final Long userId) {
        final Map<Long, SegmentFilterUserPreference> orgPreferences = preferenceCache.get(orgId);

        return Objects.isNull(orgPreferences) ? null : orgPreferences.get(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final SegmentFilterUserPreference preference) {
        preferenceCache.computeIfAbsent(preference.orgId(), orgId -> new HashMap<>()).put(preference.getUserId(), preference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(final Long orgId, final Long userId) {
        final Map<Long, SegmentFilterUserPreference> orgPreferences = preferenceCache.get(orgId);

        if (Objects.nonNull(orgPreferences)) {
            orgPreferences.remove(userId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        preferenceCache.clear();
    }
}
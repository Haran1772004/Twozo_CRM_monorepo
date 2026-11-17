package com.tlc.crm.common.internal.export.cache.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.crm.common.export.req.ExportTracker;
import com.tlc.crm.common.internal.export.cache.ExportTrackerCache;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Holds information about the export requests in the cache
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class ExportTrackerCacheImpl implements ExportTrackerCache {

    private final Cache<Long, ExportTracker> exportStatusCache;

    private ExportTrackerCacheImpl() {
        final CacheConfig<Long, ExportTracker> remoteConfig = new CacheConfig<>("DATA_EXPORT");

        remoteConfig.setTtl((int) TimeUnit.MINUTES.toMillis(15));
        exportStatusCache = CacheServiceProvider.get().createOrOpenCache(remoteConfig);
    }

    private static class Instance {
        private static final ExportTrackerCacheImpl INSTANCE = new ExportTrackerCacheImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ExportTrackerCacheImpl}.
     * </p>
     *
     * @return {@link ExportTrackerCacheImpl}}
     */
    public static ExportTrackerCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {

    }

    /**
     * {@inheritDoc}
     *
     * @param exportId Export action identifier
     * @return {@link ExportTracker}
     */
    @Override
    public Optional<ExportTracker> get(final Long exportId) {
        return Optional.ofNullable(exportStatusCache.get(exportId));
    }

    /**
     * {@inheritDoc}
     *
     * @param exportId   Export action identifier
     * @param dataExport Export status
     */
    @Override
    public void put(final Long exportId, final ExportTracker dataExport) {
        exportStatusCache.put(exportId, dataExport);
    }

    /**
     * {@inheritDoc}
     *
     * @param exportId Export action identifier
     */
    @Override
    public void remove(final Long exportId) {
        exportStatusCache.remove(exportId);
    }
}

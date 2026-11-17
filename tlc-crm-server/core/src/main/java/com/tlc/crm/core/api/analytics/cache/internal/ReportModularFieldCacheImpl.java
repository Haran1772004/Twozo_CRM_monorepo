package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.cache.AsyncCache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.commons.listeners.Callback;
import com.tlc.crm.core.api.analytics.cache.executor.AnalyticsCacheExecutorProvider;
import com.tlc.crm.core.api.analytics.cache.module.ReportModularFieldCache;
import com.tlc.crm.core.api.analytics.cache.util.CacheKeyUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ReportModularFieldCache} using remote cache with TTL.
 * Uses composed keys ({@code "orgId:entityId"}) for tenant isolation.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ReportModularFieldCacheImpl implements ReportModularFieldCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportModularFieldCacheImpl.class);

    private final AsyncCache<Long, Collection<ReportModularField>> reportModularFieldCache;
    private final AsyncCache<String, ReportModularField> reportModularFieldByIdCache;

    private ReportModularFieldCacheImpl() {
        final Executor asyncExecutor = AnalyticsCacheExecutorProvider.get();

        this.reportModularFieldCache = createAsyncCache("reportModularField", asyncExecutor);
        this.reportModularFieldByIdCache = createAsyncCache("reportModularFieldById", asyncExecutor);
    }

    private static class Instance {
        private static final ReportModularFieldCacheImpl INSTANCE = new ReportModularFieldCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link ReportModularFieldCacheImpl}.
     * </p>
     *
     * @return The {@link ReportModularFieldCacheImpl} instance
     */
    public static ReportModularFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private static <K, V> AsyncCache<K, V> createAsyncCache(final String cacheName, final Executor asyncExecutor) {
        final CacheConfig<K, V> cacheConfig = new CacheConfig<>(cacheName);

        cacheConfig.setTtl((int) TimeUnit.HOURS.toMillis(NumericalConstant.THREE));
        cacheConfig.setTaskExecutor(asyncExecutor);

        return CacheServiceProvider.get().createOrOpenAsyncCache(cacheConfig);
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
    public void getAsync(final Long orgId, final Callback<Collection<ReportModularField>> callback) {
        reportModularFieldCache.getAsync(orgId, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getByIdAsync(final Long orgId, final Long fieldId, final Callback<ReportModularField> callback) {
        reportModularFieldByIdCache.getAsync(CacheKeyUtil.compose(orgId, fieldId), callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAsync(final Long orgId, final Collection<ReportModularField> data,
                         final Callback<Map.Entry<Long, Collection<ReportModularField>>> callback) {
        reportModularFieldCache.putAsync(orgId, data, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putByIdAsync(final Long orgId, final Long fieldId, final ReportModularField entity,
                             final Callback<Map.Entry<String, ReportModularField>> callback) {
        reportModularFieldByIdCache.putAsync(CacheKeyUtil.compose(orgId, fieldId), entity, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCache(final Long orgId) {
        removeBySourceIdEntry(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCache(final Long orgId, final Collection<Long> fieldIds) {
        removeAllByIdEntries(orgId, new HashSet<>(fieldIds));
        removeBySourceIdEntry(orgId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void invalidateCacheBySourceId(final Long orgId) {
        removeBySourceIdEntry(orgId);
    }

    /**
     * <p>
     * Removes all by-ID cache entries for the given org and field IDs.
     * </p>
     *
     * @param orgId    the organization ID
     * @param fieldIds the field IDs to invalidate
     */
    private void removeAllByIdEntries(final Long orgId, final Set<Long> fieldIds) {
        final Set<String> composedKeys = fieldIds.stream()
                .map(id -> CacheKeyUtil.compose(orgId, id))
                .collect(Collectors.toSet());

        reportModularFieldByIdCache.removeAllAsync(composedKeys, new Callback<>() {
            @Override
            public void onSuccess(final ReportModularField ignored) {
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.warn("CRM Analytics : Failed to invalidate reportModularFieldById cache for ids {}", fieldIds, cause);
            }
        });
    }

    /**
     * <p>
     * Removes the source-ID-based cache entry for the given org.
     * </p>
     *
     * @param orgId the organization ID
     */
    private void removeBySourceIdEntry(final Long orgId) {
        reportModularFieldCache.removeAsync(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Collection<ReportModularField> ignored) {
                LOGGER.debug("CRM Analytics : Invalidated ReportModularField cache for key [{}]", orgId);
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.warn("CRM Analytics : Failed to invalidate ReportModularField cache for key [{}]", orgId, cause);
            }
        });
    }
}

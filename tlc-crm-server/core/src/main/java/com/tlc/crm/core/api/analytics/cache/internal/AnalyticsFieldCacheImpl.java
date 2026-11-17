package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.core.api.analytics.cache.field.AnalyticsFieldCache;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.manager.config.field.AnalyticsFieldManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link AnalyticsFieldCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class AnalyticsFieldCacheImpl implements AnalyticsFieldCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsFieldCacheImpl.class);
    private final Cache<Long, AnalyticsField> analyticsFieldCache;
    private final Cache<Integer, Collection<AnalyticsField>> cacheByModuleId;
    private final AnalyticsFieldManager analyticsFieldManager;

    private AnalyticsFieldCacheImpl() {
        this.analyticsFieldCache = CacheManager.getInstance().createCache();
        this.cacheByModuleId = CacheManager.getInstance().createCache();
        this.analyticsFieldManager = AnalyticsFieldManager.INSTANCE();
    }

    private static class Instance {
        private static final AnalyticsFieldCacheImpl INSTANCE = new AnalyticsFieldCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link AnalyticsFieldCacheImpl} instance
     * </p>
     *
     * @return {@link AnalyticsFieldCacheImpl}
     */
    public static AnalyticsFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        analyticsFieldManager.fetchAll().forEach(field -> {
            analyticsFieldCache.put(field.id(), field);

            final Integer moduleId = field.getModuleId();
            cacheByModuleId.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(field);
        });

        LOGGER.info("CRM Analytics : Analytics fields loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        analyticsFieldCache.clear();
        cacheByModuleId.clear();
        LOGGER.info("CRM Analytics : AnalyticsField cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AnalyticsField> getByModuleId(final Integer moduleId) {
        final Collection<AnalyticsField> result = cacheByModuleId.get(moduleId);

        return Objects.nonNull(result) ? result : new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalyticsField get(final Long fieldId) {
        return analyticsFieldCache.get(fieldId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final AnalyticsField field) {
        analyticsFieldCache.put(field.id(), field);
        cacheByModuleId.computeIfAbsent(field.getModuleId(), k -> new ArrayList<>()).add(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(final Collection<AnalyticsField> fields) {
        fields.forEach(this::set);
    }

}

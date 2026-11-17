package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.cache.field.AnalyticsFieldCache;
import com.tlc.crm.core.api.analytics.entity.config.fields.AnalyticsField;
import com.tlc.crm.core.api.analytics.manager.config.field.AnalyticsFieldManager;
import com.tlc.crm.core.api.analytics.resolver.field.AnalyticsFieldResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link AnalyticsFieldResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB via {@link AnalyticsFieldManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class AnalyticsFieldResolverImpl implements AnalyticsFieldResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsFieldResolverImpl.class);

    private final AnalyticsFieldCache analyticsFieldCache;
    private final AnalyticsFieldManager analyticsFieldManager;

    private AnalyticsFieldResolverImpl() {
        this.analyticsFieldCache = AnalyticsFieldCache.getInstance();
        this.analyticsFieldManager = AnalyticsFieldManager.INSTANCE();
    }

    private static class Instance {
        private static final AnalyticsFieldResolverImpl INSTANCE = new AnalyticsFieldResolverImpl();
    }

    /**
     * <p>
     * Retrieves the {@link AnalyticsFieldResolverImpl} instance
     * </p>
     *
     * @return {@link AnalyticsFieldResolverImpl}
     */
    public static AnalyticsFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AnalyticsField> get(final Integer moduleId) {
        final Collection<AnalyticsField> cached = analyticsFieldCache.getByModuleId(moduleId);

        if (!cached.isEmpty()) {
            return cached;
        }

        final Collection<AnalyticsField> fromDB = analyticsFieldManager.fetchByModuleId(moduleId);

        if (!fromDB.isEmpty()) {
            analyticsFieldCache.setAll(fromDB);
        }

        return fromDB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnalyticsField getById(final Long fieldId) {
        final AnalyticsField cached = analyticsFieldCache.get(fieldId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final AnalyticsField fromDB = analyticsFieldManager.fetchByFieldId(fieldId);

        if (Objects.nonNull(fromDB)) {
            analyticsFieldCache.set(fromDB);
        }

        return fromDB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, AnalyticsField> getByIds(final Collection<Long> fieldIds) {
        final Map<Long, AnalyticsField> result = new HashMap<>();

        for (final Long fieldId : fieldIds) {
            final AnalyticsField cached = analyticsFieldCache.get(fieldId);

            if (Objects.nonNull(cached)) {
                result.put(fieldId, cached);
            } else {
                final AnalyticsField fromDB = analyticsFieldManager.fetchByFieldId(fieldId);

                if (Objects.nonNull(fromDB)) {
                    analyticsFieldCache.set(fromDB);
                    result.put(fieldId, fromDB);
                } else {
                    LOGGER.warn("Analytics: AnalyticsField not found for fieldId: {}", fieldId);
                }
            }
        }

        return result;
    }
}

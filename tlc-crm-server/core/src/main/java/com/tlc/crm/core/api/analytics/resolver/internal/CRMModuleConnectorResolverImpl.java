package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.cache.module.CRMModuleConnectorCache;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.manager.config.module.CRMModuleConnectorManager;
import com.tlc.crm.core.api.analytics.resolver.module.CRMModuleConnectorResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMModuleConnectorResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB via {@link CRMModuleConnectorManager} and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMModuleConnectorResolverImpl implements CRMModuleConnectorResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMModuleConnectorResolverImpl.class);

    private final CRMModuleConnectorCache connectorCache;
    private final CRMModuleConnectorManager connectorManager;

    private CRMModuleConnectorResolverImpl() {
        this.connectorCache = CRMModuleConnectorCache.getInstance();
        this.connectorManager = CRMModuleConnectorManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMModuleConnectorResolverImpl INSTANCE = new CRMModuleConnectorResolverImpl();
    }

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnectorResolverImpl} instance
     * </p>
     *
     * @return {@link CRMModuleConnectorResolverImpl}
     */
    public static CRMModuleConnectorResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMModuleConnector> get(final Integer metricsModuleId) {
        final Collection<CRMModuleConnector> cached = connectorCache.getByMetricsModuleId(metricsModuleId);

        if (!cached.isEmpty()) {
            return cached;
        }

        final Collection<CRMModuleConnector> fromDB = connectorManager.fetchByMetricsModuleId(metricsModuleId);

        if (!fromDB.isEmpty()) {
            connectorCache.setAll(fromDB);
        }

        return fromDB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMModuleConnector getById(final Long connectorId) {
        final CRMModuleConnector cached = connectorCache.get(connectorId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final CRMModuleConnector fromDB = connectorManager.fetchByConnectorId(connectorId);

        if (Objects.nonNull(fromDB)) {
            connectorCache.set(fromDB);
        }

        return fromDB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId) {
        final Collection<Long> cached = connectorCache.getColumnIds(metricsModuleId, viewByModuleId);

        if (!cached.isEmpty()) {
            return cached;
        }

        // Ensure connectors are loaded into cache
        get(metricsModuleId);

        return connectorCache.getColumnIds(metricsModuleId, viewByModuleId);
    }
}

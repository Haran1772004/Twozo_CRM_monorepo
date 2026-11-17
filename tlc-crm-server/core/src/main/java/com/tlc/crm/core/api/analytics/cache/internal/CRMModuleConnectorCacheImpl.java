package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMModuleConnectorCache;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.manager.config.module.CRMModuleConnectorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMModuleConnectorCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMModuleConnectorCacheImpl implements CRMModuleConnectorCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMModuleConnectorCacheImpl.class);
    private final Cache<Long, CRMModuleConnector> connectorCache;
    private final Cache<Integer, Collection<CRMModuleConnector>> cacheByMetricsModuleId;
    private final CRMModuleConnectorManager crmModuleConnectorManager;

    private CRMModuleConnectorCacheImpl() {
        this.connectorCache = CacheManager.getInstance().createCache();
        this.cacheByMetricsModuleId = CacheManager.getInstance().createCache();
        this.crmModuleConnectorManager = CRMModuleConnectorManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMModuleConnectorCacheImpl INSTANCE = new CRMModuleConnectorCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnectorCacheImpl} instance
     * </p>
     *
     * @return {@link CRMModuleConnectorCacheImpl}
     */
    public static CRMModuleConnectorCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        crmModuleConnectorManager.fetchAll().forEach(connector -> {
            connectorCache.put(connector.id(), connector);

            final Integer metricsModuleId = connector.getMetricsModuleId();
            cacheByMetricsModuleId.computeIfAbsent(metricsModuleId, k -> new ArrayList<>()).add(connector);
        });

        LOGGER.info("CRM Analytics : Module connectors loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        connectorCache.clear();
        cacheByMetricsModuleId.clear();
        LOGGER.info("CRM Analytics : CRMModuleConnector cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMModuleConnector> getByMetricsModuleId(final Integer metricsModuleId) {
        final Collection<CRMModuleConnector> result = cacheByMetricsModuleId.get(metricsModuleId);

        return Objects.nonNull(result) ? result : new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMModuleConnector get(final Long connectorId) {
        return connectorCache.get(connectorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final CRMModuleConnector connector) {
        connectorCache.put(connector.id(), connector);
        cacheByMetricsModuleId.computeIfAbsent(connector.getMetricsModuleId(), k -> new ArrayList<>()).add(connector);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId) {
        final Collection<CRMModuleConnector> connectors = getByMetricsModuleId(metricsModuleId);
        final Collection<Long> columnIds = new ArrayList<>();

        for (final CRMModuleConnector connector : connectors) {
            if (Objects.equals(viewByModuleId, connector.getViewByModuleId())) {
                columnIds.add(connector.getColumnId());
            }
        }

        return columnIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAll(final Collection<CRMModuleConnector> connectors) {
        connectors.forEach(this::set);
    }

}

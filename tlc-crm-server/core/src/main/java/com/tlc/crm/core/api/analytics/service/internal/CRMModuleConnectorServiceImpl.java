package com.tlc.crm.core.api.analytics.service.internal;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.resolver.module.CRMModuleConnectorResolver;
import com.tlc.crm.core.api.analytics.service.module.CRMModuleConnectorService;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Implementation of {@link CRMModuleConnectorService} backed by {@link CRMModuleConnectorResolver}.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMModuleConnectorServiceImpl implements CRMModuleConnectorService {

    private final CRMModuleConnectorResolver connectorResolver;

    private CRMModuleConnectorServiceImpl() {
        this.connectorResolver = CRMModuleConnectorResolver.getInstance();
    }

    private static class Instance {
        private static final CRMModuleConnectorServiceImpl INSTANCE = new CRMModuleConnectorServiceImpl();
    }

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnectorServiceImpl} instance
     * </p>
     *
     * @return {@link CRMModuleConnectorServiceImpl}
     */
    public static CRMModuleConnectorServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMModuleConnector> getModuleConnectors(final Integer metricsModuleId) {
        if (Objects.isNull(metricsModuleId)) {
            return Collections.emptyList();
        }

        return connectorResolver.get(metricsModuleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMModuleConnector getModuleConnectorById(final Long connectorId) {
        if (Objects.isNull(connectorId)) {
            return null;
        }

        return connectorResolver.getById(connectorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId) {
        if (Objects.isNull(metricsModuleId) || Objects.isNull(viewByModuleId)) {
            return Collections.emptyList();
        }

        return connectorResolver.getColumnIds(metricsModuleId, viewByModuleId);
    }
}

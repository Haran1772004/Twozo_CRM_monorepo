package com.tlc.crm.core.api.analytics.resolver.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.resolver.internal.CRMModuleConnectorResolverImpl;

import java.util.Collection;

/**
 * Resolver interface for {@link CRMModuleConnector} that handles cache-miss resolution.
 * Checks cache first; on miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMModuleConnectorResolver {

    /**
     * <p>
     * Retrieves the instance of {@link CRMModuleConnectorResolver}
     * </p>
     *
     * @return The {@link CRMModuleConnectorResolver} instance
     */
    static CRMModuleConnectorResolver getInstance() {
        return CRMModuleConnectorResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMModuleConnector} for the given metrics module id.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @return {@link Collection} of {@link CRMModuleConnector}
     */
    Collection<CRMModuleConnector> get(final Integer metricsModuleId);

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnector} for the given connector id.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param connectorId Represents the connector id
     * @return {@link CRMModuleConnector}
     */
    CRMModuleConnector getById(final Long connectorId);

    /**
     * <p>
     * Retrieves the column IDs for the given metrics module id and view-by module id.
     * If not found in cache, fetches from DB and populates cache.
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @param viewByModuleId  Represents the view-by module id
     * @return {@link Collection} of column IDs
     */
    Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId);
}

package com.tlc.crm.core.api.analytics.cache.module;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.api.analytics.cache.internal.CRMModuleConnectorCacheImpl;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;

import java.util.Collection;

/**
 * Cache interface for {@link CRMModuleConnector} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMModuleConnectorCache extends LocalCache {

    /**
     * <p>
     * Retrieves the instance of {@link CRMModuleConnectorCache}
     * </p>
     *
     * @return The {@link CRMModuleConnectorCache} instance
     */
    static CRMModuleConnectorCache getInstance() {
        return CRMModuleConnectorCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMModuleConnector} from the cache for the given metrics module id
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @return {@link Collection} of {@link CRMModuleConnector}
     */
    Collection<CRMModuleConnector> getByMetricsModuleId(final Integer metricsModuleId);

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnector} from the cache for the given connector id
     * </p>
     *
     * @param connectorId Represents the connector id
     * @return {@link CRMModuleConnector}
     */
    CRMModuleConnector get(final Long connectorId);

    /**
     * <p>
     * Stores the {@link CRMModuleConnector} into the cache
     * </p>
     *
     * @param connector Represents the {@link CRMModuleConnector}
     */
    void set(final CRMModuleConnector connector);

    /**
     * <p>
     * Stores the {@link Collection} of {@link CRMModuleConnector} into the cache
     * </p>
     *
     * @param connectors Represents the {@link Collection} of {@link CRMModuleConnector}
     */
    void setAll(final Collection<CRMModuleConnector> connectors);

    /**
     * <p>
     * Retrieves the column IDs from the cache for the given metrics module id and view-by module id
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @param viewByModuleId  Represents the view-by module id
     * @return {@link Collection} of column IDs
     */
    Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId);
}

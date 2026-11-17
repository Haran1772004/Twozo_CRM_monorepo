package com.tlc.crm.core.api.analytics.service.module;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.service.internal.CRMModuleConnectorServiceImpl;
import java.util.Collection;
/**
 * Service interface for accessing {@link CRMModuleConnector} with local caching support.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMModuleConnectorService {

    /**
     * <p>
     * Retrieves the instance of {@link CRMModuleConnectorService}
     * </p>
     *
     * @return The {@link CRMModuleConnectorService} instance
     */
    static CRMModuleConnectorService getInstance() {
        return CRMModuleConnectorServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of {@link CRMModuleConnector} for the given metrics module id
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @return {@link Collection} of {@link CRMModuleConnector}
     */
    Collection<CRMModuleConnector> getModuleConnectors(final Integer metricsModuleId);

    /**
     * <p>
     * Retrieves the {@link CRMModuleConnector} for the given connector id
     * </p>
     *
     * @param connectorId Represents the connector id
     * @return {@link CRMModuleConnector}
     */
    CRMModuleConnector getModuleConnectorById(final Long connectorId);

    /**
     * <p>
     * Retrieves the column IDs for the given metrics module id and view-by module id directly from cache
     * </p>
     *
     * @param metricsModuleId Represents the metrics module id
     * @param viewByModuleId  Represents the view-by module id
     * @return {@link Collection} of column IDs
     */
    Collection<Long> getColumnIds(final Integer metricsModuleId, final Integer viewByModuleId);
}

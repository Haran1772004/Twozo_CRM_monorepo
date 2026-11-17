package com.tlc.cdc.core.manager;

import com.tlc.cdc.core.models.ConnectorConfig;

import java.util.Collection;

/**
 * Manages CDC connector configurations and provides access to registered connectors.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public interface CdcConnectorManager {

    /**
     * Returns the singleton instance of the {@code CdcConnectorManager}.
     *
     * @return The shared {@code CdcConnectorManager} instance.
     */
    static CdcConnectorManager getInstance() {
        return CdcConnectorManagerImpl.getInstance();
    }

    /**
     * Retrieves all connector configurations for the given connector type.
     *
     * @param type The connector type to filter by.
     * @return A collection of connector configurations matching the given type
     */
    Collection<ConnectorConfig> getAll(final ConnectorConfig.Type type);

    /**
     * Retrieves the alias name associated with the specified source table.
     *
     * @param tableName The name of the source table to look up.
     * @return The alias name for the given table, or {@code null} if no matching connector is found.
     */
    String getAliasName(final String tableName);
}

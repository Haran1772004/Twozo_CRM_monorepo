package com.tlc.cdc.core.connect.internal;

import com.tlc.cdc.core.connect.ConnectorConfig;

/**
 * Defines the contract for managing Kafka Connect connectors.
 *
 * @param <C> the type of {@link ConnectorConfig} used to configure the connector.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ConnectorService<C extends ConnectorConfig> {

    /**
     * Creates a new Kafka Connect connector using the provided configuration.
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    void create(final C connectorConfig);

    /**
     * Updates an existing Kafka Connect connector with the provided configuration.
     *
     * @param connectorConfig The configuration used to update the connector.
     */
    void update(final C connectorConfig);

    /**
     * Deletes the Kafka Connect connector identified by the provided configuration.
     *
     * @param connectorConfig The configuration identifying the connector to delete.
     */
    void delete(final C connectorConfig);

    /**
     * Retrieves the current status of the Kafka Connect connector.
     *
     * @param connectorConfig The configuration identifying the connector.
     */
    void status(final C connectorConfig);

    /**
     * Stops the connector service and releases any underlying resources.
     */
    void stop();
}

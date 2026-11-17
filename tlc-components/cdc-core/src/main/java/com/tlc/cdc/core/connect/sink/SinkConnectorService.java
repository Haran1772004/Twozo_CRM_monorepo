package com.tlc.cdc.core.connect.sink;

import com.tlc.cdc.core.connect.internal.AbstractConnectorService;

import com.tlc.web.HttpMethod;

/**
 * Service layer responsible for managing Kafka Connect sink connectors.
 *
 * <p>
 * This service provides high-level operations for creating, updating,
 * deleting, restarting, and querying sink connectors via the Kafka Connect
 * REST API.
 * </p>
 *
 * <p>
 * Typical use cases include:
 * <ul>
 *   <li>Provisioning sink connectors during application startup</li>
 *   <li>Restarting failed connectors or tasks</li>
 *   <li>Monitoring connector health and status</li>
 * </ul>
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SinkConnectorService extends AbstractConnectorService<SinkConnectorConfig> {

    private SinkConnectorService() {
    }

    private static class Instance {
        private static final SinkConnectorService CONNECTOR_SERVICE = new SinkConnectorService();
    }

    public static SinkConnectorService getInstance() {
        return Instance.CONNECTOR_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void create(final SinkConnectorConfig connectorConfig) {
        connect(connectorConfig.getUrl(), HttpMethod.POST, connectorConfig.toRequestJson());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void update(final SinkConnectorConfig connectorConfig) {
        connect(connectorConfig.getUrl(), HttpMethod.PUT, connectorConfig.toRequestJson());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void delete(final SinkConnectorConfig connectorConfig) {
        delete(connectorConfig.getUrl(), connectorConfig.getName());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void status(final SinkConnectorConfig connectorConfig) {
        status(connectorConfig.getUrl(), connectorConfig.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

    }

}

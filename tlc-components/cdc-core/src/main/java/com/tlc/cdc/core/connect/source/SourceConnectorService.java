package com.tlc.cdc.core.connect.source;

import com.tlc.cdc.core.connect.internal.AbstractConnectorService;

import com.tlc.web.HttpMethod;

/**
 * Service layer responsible for managing Kafka Connect source connectors.
 * <p>
 * Typical use cases include:
 * <ul>
 *   <li>Provisioning source connectors during application startup</li>
 *   <li>Restarting failed connectors or tasks</li>
 *   <li>Monitoring connector health and status</li>
 * </ul>
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class SourceConnectorService extends AbstractConnectorService<SourceConnectorConfig> {

    private SourceConnectorService() {
    }

    private static class Instance {
        private static final SourceConnectorService CONNECTOR_SERVICE = new SourceConnectorService();
    }

    public static SourceConnectorService getInstance() {
        return Instance.CONNECTOR_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void create(final SourceConnectorConfig connectorConfig) {
        connect(connectorConfig.getUrl(), HttpMethod.POST, connectorConfig.toRequestJson());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void update(final SourceConnectorConfig connectorConfig) {
        connect(connectorConfig.getUrl(), HttpMethod.PUT, connectorConfig.toRequestJson());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void delete(final SourceConnectorConfig connectorConfig) {
        delete(connectorConfig.getUrl(), connectorConfig.getName());
    }

    /**
     * {@inheritDoc}
     *
     * @param connectorConfig The configuration used to create the connector.
     */
    @Override
    public void status(final SourceConnectorConfig connectorConfig) {
        status(connectorConfig.getUrl(), connectorConfig.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

    }
}

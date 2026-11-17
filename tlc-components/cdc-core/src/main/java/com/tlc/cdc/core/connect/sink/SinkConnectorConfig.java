package com.tlc.cdc.core.connect.sink;

import com.tlc.cdc.core.connect.ConnectorConfig;

/**
 * Configuration definition for the Kafka Sink Connector.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SinkConnectorConfig extends ConnectorConfig {

    public SinkConnectorConfig() {
        super(Type.SINK);
    }
}

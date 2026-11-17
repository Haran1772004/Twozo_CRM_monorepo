package com.tlc.cdc.core.connect.source;

import com.tlc.cdc.core.connect.ConnectorConfig;

/**
 * Configuration definition for the Kafka Source Connector.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SourceConnectorConfig extends ConnectorConfig {

    public SourceConnectorConfig() {
        super(Type.SOURCE);
    }
}

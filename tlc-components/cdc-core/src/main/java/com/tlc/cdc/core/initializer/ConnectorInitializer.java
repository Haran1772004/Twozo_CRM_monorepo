package com.tlc.cdc.core.initializer;

/**
 * Abstract class to registers the source and sink connectors with Kafka Connect.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ConnectorInitializer {

    /**
     * Initializes the connectors.
     */
    void initialize();
}

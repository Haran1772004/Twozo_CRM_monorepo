package com.tlc.cdc.core.connect;

import com.tlc.cdc.core.connect.internal.ConnectService;
import com.tlc.commons.util.Configurations;

import com.tlc.cdc.core.connect.sink.SinkConnectorService;
import com.tlc.cdc.core.connect.source.SourceConnectorService;

/**
 * Manages and provides access to Kafka Connect operations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ConnectOrchestrator implements ConnectService {

    private SinkConnectorService sinkConnectorService;
    private SourceConnectorService sourceConnectorService;

    private ConnectOrchestrator() {
    }

    private static class Instance {
        private static final ConnectOrchestrator CONNECTOR_SERVICE = new ConnectOrchestrator();
    }

    public static ConnectOrchestrator getInstance() {
        return Instance.CONNECTOR_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Refers the configurations.
     */
    @Override
    public void start(final Configurations configurations) {
        this.sinkConnectorService = SinkConnectorService.getInstance();
        this.sourceConnectorService = SourceConnectorService.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        sinkConnectorService.stop();
        sourceConnectorService.stop();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SinkConnectorService sinkConnector() {
        return sinkConnectorService;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SourceConnectorService sourceConnector() {
        return sourceConnectorService;
    }
}

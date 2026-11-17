package com.tlc.cdc.core.connect.internal;

import com.tlc.commons.service.Service;

import com.tlc.cdc.core.connect.sink.SinkConnectorService;
import com.tlc.cdc.core.connect.source.SourceConnectorService;

/**
 * <p>
 * Service interface that provides access to Kafka Connect operations.
 * </p>
 *
 * <p>
 * This service acts as a façade over Kafka Connect and exposes dedicated
 * services for managing <b>source</b> and <b>sink</b> connectors. Each
 * sub-service is responsible for the lifecycle management of its respective
 * connector type, such as creation, update, deletion, and status retrieval.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ConnectService extends Service {

    /**
     * Provides access to operations related to Kafka Connect <b>source</b> connectors.
     *
     * @return the {@link SourceConnectorService} for managing source connectors.
     */
    SourceConnectorService sourceConnector();

    /**
     * Provides access to operations related to Kafka Connect <b>sink</b> connectors.
     *
     * @return the {@link SinkConnectorService} for managing sink connectors.
     */
    SinkConnectorService sinkConnector();
}

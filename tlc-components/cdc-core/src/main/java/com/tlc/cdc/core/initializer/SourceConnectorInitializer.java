package com.tlc.cdc.core.initializer;

import com.tlc.cdc.core.manager.CdcConnectorManager;
import com.tlc.cdc.core.models.ConnectorConfig;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.queue.internal.TopicConfig;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.cdc.core.connect.ConnectOrchestrator;
import com.tlc.cdc.core.connect.source.SourceConnectorConfig;
import com.tlc.cdc.core.connect.source.SourceConnectorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Initializes and registers the source connector with Kafka Connect.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class SourceConnectorInitializer implements ConnectorInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceConnectorInitializer.class);

    private final Configurations configurations;
    private final SourceConnectorService sourceConnectorService;
    private final QueueService queueService;
    private final CdcConnectorManager cdcConnectorManager;

    private SourceConnectorInitializer() {
        this.sourceConnectorService = ConnectOrchestrator.getInstance().sourceConnector();
        this.queueService = QueueServiceProvider.get();
        this.cdcConnectorManager = CdcConnectorManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties("kafka-connect");
    }

    private static class Instance {
        private static final SourceConnectorInitializer SOURCE_CONNECTOR_INITIALIZER = new SourceConnectorInitializer();
    }

    static SourceConnectorInitializer getInstance() {
        return Instance.SOURCE_CONNECTOR_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final Collection<ConnectorConfig> connectorsGroup = cdcConnectorManager.getAll(ConnectorConfig.Type.SOURCE);
        final String url = configurations.getConfig("url");
        final Configurations sourceConfiguration = configurations.getNestedConfigs("connector.source");
        final Map<String, String> config = sourceConfiguration.getNestedConfigs("config").getConfigs();
        final JsonObject jsonObject = Json.object(config);
        final String schema = jsonObject.getString("schema.include.list");
        final String topicPrefix = jsonObject.getString("topic.prefix");

        for (final ConnectorConfig connectorConfig : connectorsGroup) {
            final String tables = connectorConfig.tables().stream().map(table -> String.format("%s.%s", schema, table.toLowerCase())).collect(Collectors.joining(","));
            final SourceConnectorConfig sourceConnectorConfig = new SourceConnectorConfig();
            final String connectorName = connectorConfig.name();

            jsonObject.put("table.include.list", tables);
            jsonObject.put("slot.name", String.join("_", connectorName.replace("-", "_"), "slot"));
            jsonObject.put("publication.name", String.join("_", connectorName.replace("-", "_"), "pub"));

            sourceConnectorConfig.setUrl(url);
            sourceConnectorConfig.setName(connectorName);
            sourceConnectorConfig.setConfig(jsonObject);

            createTopics(topicPrefix, tables);

            try {
                sourceConnectorService.create(sourceConnectorConfig);
                LOGGER.info("CDC: Source Connector - {} successfully connected and registered with Kafka Connect", sourceConnectorConfig.getName());
            } catch (Exception exception) {
                LOGGER.error("CDC: Failed to connect/register source connector - {}, Reason: {}", sourceConnectorConfig.getName(), exception.getMessage(), exception);
            }
        }
    }

    /**
     * Creates all the topics of the source connector.
     *
     * @param topics Refers the topics to be created.
     */
    private void createTopics(final String topicPrefix, final String topics) {
        for (final String topic : topics.split(",")) {
            try {
                queueService.createTopicIfNotExist(new TopicConfig(String.join(".", topicPrefix, topic.toLowerCase().trim()), 1));
            } catch (Exception exception) {
                LOGGER.error("Kafka connect: Failed to create topic. Reason: {}", exception.getMessage(), exception);
            }
        }
    }
}

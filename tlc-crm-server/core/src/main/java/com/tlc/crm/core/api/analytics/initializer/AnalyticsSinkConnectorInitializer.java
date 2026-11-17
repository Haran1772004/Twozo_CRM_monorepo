package com.tlc.crm.core.api.analytics.initializer;

import com.tlc.cdc.core.initializer.ConnectorInitializer;
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
import com.tlc.cdc.core.connect.sink.SinkConnectorConfig;
import com.tlc.cdc.core.connect.sink.SinkConnectorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Initializes and registers the analytics sink connector with Kafka Connect.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AnalyticsSinkConnectorInitializer implements ConnectorInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSinkConnectorInitializer.class);

    private final Configurations configurations;
    private final SinkConnectorService sinkConnectorService;
    private final QueueService queueService;
    private final CdcConnectorManager cdcConnectorManager;

    private AnalyticsSinkConnectorInitializer() {
        this.sinkConnectorService = ConnectOrchestrator.getInstance().sinkConnector();
        this.queueService = QueueServiceProvider.get();
        this.cdcConnectorManager = CdcConnectorManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties("kafka-connect");
    }

    private static class Instance {
        private static final AnalyticsSinkConnectorInitializer SINK_CONNECTOR_INITIALIZER = new AnalyticsSinkConnectorInitializer();
    }

    public static AnalyticsSinkConnectorInitializer getInstance() {
        return Instance.SINK_CONNECTOR_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final Collection<ConnectorConfig> connectorsGroup = cdcConnectorManager.getAll(ConnectorConfig.Type.SINK);
        final String url = configurations.getConfig("url");
        final Configurations sinkConfiguration = configurations.getNestedConfigs("connector.analytics.sink");
        final int topicPartition = sinkConfiguration.getInt("topic.partition");
        final Map<String, String> config = sinkConfiguration.getNestedConfigs("config").getConfigs();
        final JsonObject jsonObject = Json.object(config);

        for (final ConnectorConfig analyticsConnectorConfig : connectorsGroup) {
            final SinkConnectorConfig sinkConnectorConfig = new SinkConnectorConfig();
            final String connectorName = analyticsConnectorConfig.name();
            final Collection<String> topicList = analyticsConnectorConfig.tables();
            final String topics = topicList.stream().map(String::toLowerCase).collect(Collectors.joining(","));
            final String dlqTopicName = String.join("-", "dlq", connectorName);

            jsonObject.put("topics", topics);
            jsonObject.put("errors.deadletterqueue.topic.name", dlqTopicName);

            sinkConnectorConfig.setUrl(url);
            sinkConnectorConfig.setName(connectorName);
            sinkConnectorConfig.setConfig(jsonObject);

            createTopics(topicList, topicPartition);
            createTopics(List.of(dlqTopicName), 1);

            try {
                sinkConnectorService.create(sinkConnectorConfig);
                LOGGER.info("Analytics CDC: Sink Connector - {} successfully connected and registered with Kafka Connect", sinkConnectorConfig.getName());
            } catch (Exception exception) {
                LOGGER.error("Analytics CDC: Failed to connect/register sink connector - {}, Reason: {}", sinkConnectorConfig.getName(), exception.getMessage(), exception);
            }
        }
    }

    /**
     * Creates all the topics of the sink connector.
     *
     * @param topics    Refers the topics to be created.
     * @param partition Refers the topic partition.
     */
    private void createTopics(final Collection<String> topics, final int partition) {
        for (final String topic : topics) {
            try {
                queueService.createTopicIfNotExist(new TopicConfig(topic.toLowerCase().trim(), partition));
            } catch (Exception exception) {
                LOGGER.error("Kafka connect: Failed to create topic. Reason: {}", exception.getMessage(), exception);
            }
        }
    }
}

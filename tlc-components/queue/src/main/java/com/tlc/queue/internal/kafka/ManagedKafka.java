package com.tlc.queue.internal.kafka;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * A managed Kafka service where the environment is entirely maintained and operated externally
 *
 * @author sundar
 * @version 1.0
 */
public class ManagedKafka extends KafkaService {

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains the kafka configuration
     */
    @Override
    protected void preProcess(final Configurations configurations) {
        final Configurations managedConfigurations = configurations.getNestedConfigs("managed");
        final String[] servers = managedConfigurations.getConfig("server").split(StringUtil.Delimiter.COMMA.getDelimiter());
        final String provider = managedConfigurations.getConfig("provider").trim();
        final Configurations providerConfigurations = managedConfigurations.getNestedConfigs(provider);

        clientInput.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, StringUtil.join(StringUtil.Delimiter.COMMA, servers));
        clientInput.put("security.protocol", managedConfigurations.getConfig("security.protocol"));
        loadConfigurations(provider, providerConfigurations);
    }

    /**
     * Loads the given provider-specific settings into the Kafka client configuration.
     *
     * @param provider               The name or identifier of the provider.
     * @param providerConfigurations The configuration properties specific to the provider.
     */
    private void loadConfigurations(final String provider, final Configurations providerConfigurations) {
        clientInput.put("sasl.mechanism", providerConfigurations.getConfig("sasl.mechanism"));
        clientInput.put("sasl.jaas.config", providerConfigurations.getConfig("sasl.jaas.config"));

        switch (provider.toLowerCase()) {
            case "aws" -> clientInput.put("sasl.client.callback.handler.class", providerConfigurations.getConfig("sasl.client.callback.handler.class"));
            case "azure" -> clientInput.put("sasl.login.callback.handler.class", providerConfigurations.getConfig("sasl.login.callback.handler.class"));
            default -> throw ErrorCode.get(QueueErrorCodes.INVALID_PROVIDER);
        }
    }
}
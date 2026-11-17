package com.tlc.queue.internal.kafka;

import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * A self-managed Kafka service where the environment is entirely maintained and operated independently
 *
 * @author sundar
 * @version 1.0
 */
public class SelfManagedKafka extends KafkaService {

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains the kafka configuration
     */
    @Override
    protected void preProcess(final Configurations configurations) {
        final String[] servers = configurations.getConfig("selfManaged.server").split(StringUtil.Delimiter.COMMA.getDelimiter());

        clientInput.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, StringUtil.join(StringUtil.Delimiter.COMMA, servers));
    }
}

package com.tlc.queue;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.util.Configurations;
import com.tlc.queue.internal.cache.consumer.ConsumerGroupService;
import com.tlc.queue.internal.cache.topic.TopicService;
import com.tlc.queue.internal.manager.offset.AsyncProcessedOffsetManager;
import com.tlc.queue.internal.ResourceManager;
import com.tlc.queue.internal.kafka.SelfManagedKafka;
import com.tlc.queue.internal.kafka.ManagedKafka;
import com.tlc.queue.internal.kafka.KafkaService;
import com.tlc.queue.internal.registry.QueueServiceRegistry;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class QueueAccess implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueAccess.class);
    private final ServiceRegistry<QueueService> queueServiceRegistry = QueueServiceRegistry.getInstance();

    /**
     * Gets the queue service for the given client
     *
     * @param client Type of the client
     * @return {@link QueueService}
     */
    public static QueueService get(QueueModel.Type client) {
        return switch (client) {
            case KAFKA -> getKafkaService();
        };
    }

    /**
     * Gets kafka queue service
     *
     * @return {@link QueueService}
     */
    private static QueueService getKafkaService() {
        final QueueService service = QueueServiceProvider.get();

        if (service == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    @Override
    public void start(BundleContext bundleContext) {
        final Configurations configurations = ConfHolder.getInstance().getProperties("queue");
        final KafkaService kafkaService = switch (configurations.getConfig("provider")) {
            case "SelfManagedKafka" -> new SelfManagedKafka();
            case "ManagedKafka" -> new ManagedKafka();
            default -> throw ErrorCode.get(QueueErrorCodes.INVALID_PROVIDER);
        };

        kafkaService.start(configurations);
        queueServiceRegistry.register(kafkaService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext bundleContext) {
        queueServiceRegistry.unregister();
        AsyncProcessedOffsetManager.getInstance().shutdownExecutor();
        TopicService.getInstance().clearCache();
        ConsumerGroupService.getInstance().clearCache();
        stopKafkaService();
        ResourceManager.getInstance().releaseAllResources();
    }

    /**
     * Stops kafka service
     */
    private void stopKafkaService() {
        try {
            final QueueService queueService = QueueServiceProvider.get();

            if (Objects.nonNull(queueService)) {
                queueService.stop();
            }
        } catch (Throwable e) {
            LOGGER.error("Problem while stopping kafka service. Error - {}", e.getMessage());
        }

    }

}

package com.tlc.components.starter.initializer.components;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.queue.internal.kafka.SelfManagedKafka;
import com.tlc.queue.internal.kafka.ManagedKafka;
import com.tlc.queue.internal.kafka.KafkaService;
import com.tlc.queue.internal.registry.QueueServiceRegistry;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.service.QueueService;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the queue services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class QueueInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<QueueService> queueServiceRegistry;

    private QueueInitializer() {
        this.queueServiceRegistry = QueueServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(ComponentsStarterFields.QUEUE);

            final KafkaService kafkaService = switch (configurations.getConfig("provider")) {
                case "SelfManagedKafka" -> new SelfManagedKafka();
                case "ManagedKafka" -> new ManagedKafka();
                default -> throw ErrorCode.get(QueueErrorCodes.INVALID_PROVIDER);
            };
            kafkaService.start(configurations);
            queueServiceRegistry.register(kafkaService);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.QUEUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return queueServiceRegistry;
    }
}

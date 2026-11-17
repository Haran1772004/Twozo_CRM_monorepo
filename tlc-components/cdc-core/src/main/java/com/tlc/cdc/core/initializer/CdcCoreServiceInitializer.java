package com.tlc.cdc.core.initializer;

import com.tlc.cdc.core.consumer.AggregatedCdcEventConsumer;
import com.tlc.cdc.core.consumer.CdcConsumerController;
import com.tlc.cdc.core.scheduler.CdcEventDispatcherScheduler;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

/**
 * Core service initializer responsible for bootstrapping and configuring
 * CDC (Change Data Capture) related components during application startup.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcCoreServiceInitializer implements ServiceInitializer {

    private final AggregatedCdcEventConsumer aggregatedCdcEventConsumer;
    private final CdcEventDispatcherScheduler cdcEventDispatcherScheduler;
    private final SourceConnectorInitializer sourceConnectorInitializer;
    private final CdcConsumerController cdcConsumerController;
    private final ConfHolder confHolder;

    private CdcCoreServiceInitializer() {
        this.aggregatedCdcEventConsumer = new AggregatedCdcEventConsumer();
        this.cdcConsumerController = new CdcConsumerController();
        this.cdcEventDispatcherScheduler = CdcEventDispatcherScheduler.getInstance();
        this.sourceConnectorInitializer = SourceConnectorInitializer.getInstance();
        this.confHolder = ConfHolder.getInstance();
    }

    private static final class Instance {
        private static final CdcCoreServiceInitializer SERVICE_INITIALIZER = new CdcCoreServiceInitializer();
    }

    public static CdcCoreServiceInitializer getInstance() {
        return Instance.SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        cdcEventDispatcherScheduler.start(configurations);
        aggregatedCdcEventConsumer.start(confHolder.getProperties("aggregated-cdc-consumer"));
        sourceConnectorInitializer.initialize();
        cdcConsumerController.start(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        cdcConsumerController.stop();
        aggregatedCdcEventConsumer.stop();
        cdcEventDispatcherScheduler.stop();
    }
}

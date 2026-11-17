package com.tlc.crm.zapier.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.event.listener.registry.EventListenerRegistry;

import com.tlc.crm.zapier.internal.async.ZapTriggerEventController;
import com.tlc.crm.zapier.internal.event.ZapEventListener;
import com.tlc.crm.zapier.internal.cache.ZapierFieldCache;

/**
 * An initializer that initializes the service which needs to be started.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ZapierServiceInitializer implements ServiceInitializer {

    private final EventListenerRegistry eventListenerRegistry;
    private final ZapEventListener zapEventListener;
    private final ZapTriggerEventController zapTriggerEventController;

    private ZapierServiceInitializer() {
        this.eventListenerRegistry = EventListenerRegistry.getInstance();
        this.zapEventListener = ZapEventListener.getInstance();
        this.zapTriggerEventController = new ZapTriggerEventController();
    }

    private static final class Instance {
        private static final ZapierServiceInitializer ZAPIER_SERVICE_INITIALIZER = new ZapierServiceInitializer();
    }

    public static ZapierServiceInitializer getInstance() {
        return Instance.ZAPIER_SERVICE_INITIALIZER;
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
        eventListenerRegistry.registerPostEventListener(zapEventListener);
        zapTriggerEventController.start(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        zapTriggerEventController.stop();
        ZapierFieldCache.getInstance().clear();
    }
}

package com.tlc.crm.messenger.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.messenger.controller.FBWebhookDataLoadService;
import com.tlc.crm.messenger.controller.WAWebhookDataLoadService;
import com.tlc.crm.messenger.controller.InstagramWebhookDataLoadService;

/**
 * <p>
 * Initializes the messenger service.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public class MessengerServiceInitializer implements ServiceInitializer {

    private final FBWebhookDataLoadService fbWebhookDataLoadService;
    private final WAWebhookDataLoadService waWebhookDataLoadService;
    private final InstagramWebhookDataLoadService instagramWebhookDataLoadService;

    private MessengerServiceInitializer() {
        fbWebhookDataLoadService = FBWebhookDataLoadService.getInstance();
        waWebhookDataLoadService = WAWebhookDataLoadService.getInstance();
        instagramWebhookDataLoadService = InstagramWebhookDataLoadService.getInstance();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link MessengerServiceInitializer}
     * </p>
     */
    private static final class InstanceHolder {
        private static final MessengerServiceInitializer CORE_SERVICE_INITIALIZER = new MessengerServiceInitializer();
    }

    /**
     * <p>
     * Gets the instance of {@link MessengerServiceInitializer}
     * </p>
     *
     * @return the instance of {@link MessengerServiceInitializer}
     */
    public static MessengerServiceInitializer getInstance() {
        return InstanceHolder.CORE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations {@link Configurations}.
     */
    @Override
    public void preInitialize(final Configurations configurations) {

    }

    /**
     * {@inheritDoc}
     *
     * @param configurations {@link Configurations}.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        fbWebhookDataLoadService.start(configurations);
        waWebhookDataLoadService.start(configurations);
        instagramWebhookDataLoadService.start(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        fbWebhookDataLoadService.stop();
        waWebhookDataLoadService.stop();
        instagramWebhookDataLoadService.stop();
    }
}

package com.tlc.web.client.internal.cluster;

import com.tlc.web.client.event.CodecRegistrar;
import com.tlc.web.client.event.EventConsumerRegistrar;
import com.tlc.web.client.registrar.WebClientResourceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restores event-bus codecs and listeners after an in-JVM Vert.x reboot via public facades only.
 *
 * @author sundar
 * @version 1.0
 */
public final class ClusterResourceRestorer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterResourceRestorer.class);

    /**
     * Drops local consumer handles without waiting for cluster unregister propagation.
     * Must be used during reboot teardown — {@code unregisterAll()} can hang when ZooKeeper is down.
     * Retains {@link WebClientResourceRegistrar} metadata so {@link #restoreAfterJoin()} can rebind.
     */
    public void discardConsumersForTeardown() {
        LOGGER.info("Discarding local event consumers for Vert.x reboot teardown (no cluster wait)");
        EventConsumerRegistrar.get().discardAllConsumers();
    }

    /**
     * Rebinds codecs and re-registers event listeners after a successful cluster rejoin.
     */
    public void restoreAfterJoin() {
        LOGGER.info("Restoring event-bus codecs and listeners after Vert.x rejoin");
        EventConsumerRegistrar.get().discardAllConsumers();
        CodecRegistrar.get().rebindAll();
        WebClientResourceRegistrar.getInstance().reregisterAll();
    }
}

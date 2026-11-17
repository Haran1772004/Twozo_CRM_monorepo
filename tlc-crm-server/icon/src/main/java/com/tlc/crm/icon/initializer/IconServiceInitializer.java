package com.tlc.crm.icon.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.icon.internal.cache.CacheLoader;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class IconServiceInitializer implements ServiceInitializer {

    private final CacheLoader cacheLoader;

    private IconServiceInitializer() {
        cacheLoader = CacheLoader.getInstance();
    }

    private static final class Instance {
        private static final IconServiceInitializer ICON_SERVICE_INITIALIZER = new IconServiceInitializer();
    }

    public static IconServiceInitializer getInstance() {
        return Instance.ICON_SERVICE_INITIALIZER;
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
        cacheLoader.load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        cacheLoader.clear();
    }
}

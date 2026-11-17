package com.tlc.crm.fields.service;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.fields.cache.internal.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class FieldsServiceInitializer implements ServiceInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldsServiceInitializer.class);

    private final CacheLoader cacheLoader;

    private FieldsServiceInitializer() {
        cacheLoader = CacheLoader.getInstance();
    }

    private static final class Instance {
        private static final FieldsServiceInitializer FIELDS_SERVICE_INITIALIZER = new FieldsServiceInitializer();
    }

    public static FieldsServiceInitializer getInstance() {
        return Instance.FIELDS_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        LOGGER.info("fields initializer started");
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

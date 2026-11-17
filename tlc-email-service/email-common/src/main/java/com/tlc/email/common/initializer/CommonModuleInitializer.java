package com.tlc.email.common.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.publish.EmailKafkaProducer;

/**
 * <p>
 * Initialization process is triggered during the application startup. Ensure that
 * all required properties and configurations for the email service are defined
 * before the application context is loaded to prevent initialization errors.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ServiceInitializer
 */
public final class CommonModuleInitializer implements ServiceInitializer {

    private CommonModuleInitializer() {
    }

    /**
     * <p>
     * Holds the single instance of {@link CommonModuleInitializer}.
     * </p>
     */
    private static class Instance {
        private static final CommonModuleInitializer COMMON_MODULE_INITIALIZER = new CommonModuleInitializer();
    }

    public static CommonModuleInitializer getInstance() {
        return Instance.COMMON_MODULE_INITIALIZER;
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        EmailKafkaProducer.getInstance().close();
    }
}

package com.tlc.attachment.initializer;

import com.tlc.attachment.generator.internal.ExternalServiceInitializer;
import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

/**
 * <p>
 * Initialization process is triggered during the application startup. Ensure that
 * all required properties and configurations for the attachment service are defined
 * before the application context is loaded to prevent initialization errors.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ServiceInitializer
 */
public final class AttachmentModuleInitializer implements ServiceInitializer {

    private final ExternalServiceInitializer externalServiceInitializer;

    private AttachmentModuleInitializer() {
        externalServiceInitializer = ExternalServiceInitializer.getInstance();
    }

    /**
     * <p>
     * Holds the single instance of {@link AttachmentModuleInitializer}.
     * </p>
     */
    private static class Instance {
        private static final AttachmentModuleInitializer COMMON_MODULE_INITIALIZER = new AttachmentModuleInitializer();
    }

    public static AttachmentModuleInitializer getInstance() {
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
        externalServiceInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        externalServiceInitializer.stop();
    }
}

package com.tlc.commons.service.initializer;

import com.tlc.commons.util.Configurations;

/**
 * <p>
 * An initializer that initializes the service which needs to be started.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface ServiceInitializer {

    /**
     * <p>
     * Initializes the service which needs to be started initially.
     * </p>
     *
     * @param configurations Holds the configuration properties.
     */
    void preInitialize(final Configurations configurations);

    /**
     * <p>
     * Initializes the service which needs to be started.
     * </p>
     *
     * @param configurations Holds the configuration properties.
     */
    void postInitialize(final Configurations configurations);

    /**
     * <p>
     * Stops the initialized service.
     * </p>
     */
    void stop();
}

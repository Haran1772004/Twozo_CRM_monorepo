package com.tlc.commons.service;

import com.tlc.commons.util.Configurations;

/**
 * Base interface for all application services that have a managed lifecycle.
 *
 * @author Abishek
 * @version 1.0
 */
public interface Service
{
    /**
     * Starts the service using the provided configuration.
     *
     * @param configurations the configuration properties required to initialize the service
     */
    void start(final Configurations configurations);

    /**
     * Stops the service, releasing all acquired resources.
     */
    void stop();
}

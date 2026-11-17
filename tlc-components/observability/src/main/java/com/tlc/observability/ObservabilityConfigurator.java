package com.tlc.observability;

import com.tlc.observability.config.ObservabilityConfig;
import com.tlc.observability.internal.ObservabilityService;

/**
 * ObservabilityConfigurator is responsible for configuring and initializing the observability services
 * in the application. It uses the provided configuration to set up the observability metrics and tracing services
 * necessary for monitoring and tracing application behavior.
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public class ObservabilityConfigurator {

    /**
     * Configures the observability service using the provided configuration.
     * This method initializes the ObservabilityService with the given ObservabilityConfig.
     *
     * @param observabilityConfig the configuration used to initialize the observability service
     */
    public static void configure(final ObservabilityConfig observabilityConfig) {
        new ObservabilityService(observabilityConfig);
    }


}

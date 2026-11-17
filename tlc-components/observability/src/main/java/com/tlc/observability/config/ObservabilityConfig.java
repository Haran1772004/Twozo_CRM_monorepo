package com.tlc.observability.config;

/**
 * Configuration class for setting up observability in an application.
 *
 * <p>
 * This record encapsulates the configuration required to enable observability,
 * including tracing and metrics. It provides settings for service information,
 * environment details, and exporter endpoints, as well as optional configurations
 * for metrics and tracing.
 * </p>
 *
 * <p>
 * <b>Default Behavior:</b> When using the constructor without the {@code MetricsConfig}
 * and {@code TracerConfig} parameters, default configurations are applied for metrics
 * and tracing using {@link MetricsConfig} and {@link TracerConfig}.
 * </p>
 *
 * <p>
 * <b>Example usage:</b>
 * </p>
 *
 * <pre>
 * ObservabilityConfig config = new ObservabilityConfig("my-service", "dev", "http://localhost:4317", "my-prefix");
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public record ObservabilityConfig(String serviceName, String environmentName, String exporterEndpoint, String keyPrefix, MetricsConfig metricsConfig, TracerConfig tracerConfig) {

    /**
     * Constructor for creating an ObservabilityConfig with default metrics and tracer configurations.
     *
     * <p>
     * Initializes the configuration with provided values for service name, environment name,
     * exporter endpoint, and key prefix. Default instances of {@link MetricsConfig} and
     * {@link TracerConfig} are used for metrics and tracing, respectively.
     * </p>
     *
     * @param serviceName      the name of the service for which observability is configured
     * @param environmentName  the name of the deployment environment (e.g., dev, prod)
     * @param exporterEndpoint the endpoint for exporting observability data (e.g., OpenTelemetry Collector URL)
     * @param keyPrefix        the prefix to be used for identifying metrics and tracing keys. This prefix will be added to all attribute keys
     */
    public ObservabilityConfig(String serviceName, String environmentName, String exporterEndpoint, String keyPrefix) {
        this(serviceName, environmentName, exporterEndpoint, keyPrefix, new MetricsConfig(), new TracerConfig());
    }

}

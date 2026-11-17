package com.tlc.observability.config;

/**
 * Configuration class for managing metrics export settings.
 *
 * <p>
 * This class is a Java record that encapsulates the configuration required
 * for exporting application metrics. It supports specifying the exporter type
 * and the interval at which metrics are exported.
 * </p>
 *
 * <p>
 * By default, the configuration uses the OpenTelemetry exporter (`ExporterType.OTEL`)
 * with an export interval of 5 seconds.
 * </p>
 *
 * <p>
 * <b>Example usage:</b>
 * </p>
 *
 * <pre>
 * MetricsConfig config = new MetricsConfig(ExporterType.LOG, 10);
 * System.out.println(config.exporterType());  // Output: LOG
 * System.out.println(config.exportIntervalInSeconds());  // Output: 10
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public record MetricsConfig(ExporterType exporterType, int exportIntervalInSeconds) {

    /**
     * Default constructor for MetricsConfig.
     *
     * <p>
     * Initializes the configuration with default values:
     * <ul>
     *   <li>Exporter type: {@code ExporterType.OTEL}</li>
     *   <li>Export interval: 5 seconds</li>
     * </ul>
     * </p>
     */
    public MetricsConfig() {
        this(ExporterType.OTEL, 5);
    }

}

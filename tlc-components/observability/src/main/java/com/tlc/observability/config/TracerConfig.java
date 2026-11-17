package com.tlc.observability.config;

/**
 * Configuration class for setting up tracing in an application.
 *
 * <p>
 * This record defines the configuration required for tracing, specifically the type of exporter
 * to be used for sending trace data. Tracing helps in tracking the execution flow and performance
 * of operations across distributed systems.
 * </p>
 *
 * <p>
 * <b>Default Behavior:</b> When using the no-argument constructor, the exporter type is set
 * to {@link ExporterType#OTEL}.
 * </p>
 *
 * <p>
 * <b>Example usage:</b>
 * </p>
 *
 * <pre>
 * TracerConfig config = new TracerConfig();
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public record TracerConfig(ExporterType exporterType) {

    /**
     * Default constructor for TracerConfig.
     *
     * <p>
     * Initializes the tracer configuration with the exporter type set to
     * {@link ExporterType#OTEL}.
     * </p>
     */
    public TracerConfig() {
        this(ExporterType.OTEL);
    }
}

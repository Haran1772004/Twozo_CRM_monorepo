package com.tlc.observability.config;

/**
 * Enum representing the types of exporters for telemetry data.
 *
 * <p>
 * Exporters are used to send telemetry data (e.g., traces, metrics, logs)
 * from an application to a specified destination for analysis or storage.
 * This enum defines the supported exporter types.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * ExporterType exporter = ExporterType.OTEL;
 * switch (exporter) {
 *     case LOG:
 *         // Handle log-based exporting
 *         break;
 *     case OTEL:
 *         // Handle OpenTelemetry-based exporting
 *         break;
 * }
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public enum ExporterType {

    /**
     * Exporter type for logging telemetry data.
     *
     * <p>
     * This type of exporter outputs telemetry data to application logs,
     * typically for debugging or local testing purposes and not recommended for production environments
     * </p>
     */
    LOG,

    /**
     * Exporter type for OpenTelemetry-based exporting.
     *
     * <p>
     * This type of exporter sends telemetry data to an OpenTelemetry
     * collector or other compatible backends for centralized monitoring
     * and analysis.
     * </p>
     */
    OTEL
}

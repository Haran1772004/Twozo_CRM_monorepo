package com.tlc.observability.internal;

import com.tlc.observability.config.ExporterType;
import com.tlc.observability.config.MetricsConfig;
import com.tlc.observability.config.ObservabilityConfig;
import com.tlc.observability.internal.utils.CommonUtil;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ObservabilityService manages the integration of OpenTelemetry metrics and tracing for observability purposes.
 * It initializes the OpenTelemetry SDK, configures metric and trace exporters, and provides access to the Meter and Tracer instances.
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
public class ObservabilityService {


    private final Meter meter;
    private final Tracer tracer;

    private final String serviceName;
    private static final AtomicReference<ObservabilityService> SERVICE_ATOMIC_REFERENCE = new AtomicReference<>();

    /**
     * Constructor for initializing the ObservabilityService.
     * This method initializes OpenTelemetry with the provided configuration and sets up metric and span exporters.
     *
     * @param observabilityConfig the configuration used to initialize the observability service
     */
    public ObservabilityService(final ObservabilityConfig observabilityConfig) {
        if (SERVICE_ATOMIC_REFERENCE.get() == null) {
            final String exporterEndPoint = observabilityConfig.exporterEndpoint();
            final MetricsConfig metricsConfig = observabilityConfig.metricsConfig();
            final String environment = observabilityConfig.environmentName();
            serviceName = observabilityConfig.serviceName();
            final Resource resource = Resource.getDefault().toBuilder()
                    .put("service.name", serviceName)
                    .put("environment.name", environment)
                    .build();

            final MetricExporter loggingExporter;

            if (metricsConfig.exporterType() == ExporterType.LOG) {
                loggingExporter = LoggingMetricExporter.create();
            } else {
                loggingExporter = OtlpGrpcMetricExporter.builder()
                        .setEndpoint(exporterEndPoint)
                        .build();
            }

            final SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                    .registerMetricReader(
                            PeriodicMetricReader.builder(loggingExporter)
                                    .setInterval(Duration.ofSeconds(metricsConfig.exportIntervalInSeconds()))
                                    .build())
                    .build();


            final SpanExporter spanExporter;

            if (observabilityConfig.tracerConfig().exporterType() == ExporterType.LOG) {
                spanExporter = LoggingSpanExporter.create();
            } else {
                spanExporter = OtlpGrpcSpanExporter.builder()
                        .setEndpoint(exporterEndPoint)
                        .build();
            }

            final BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter).build();
            final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .addResource(resource)
                    .addSpanProcessor(spanProcessor)
                    .build();

            final OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    //.setTracerProvider(tracerProvider)
                    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())) // TODO Add propagators
                    .build();


            meter = openTelemetry.getMeter(String.format("%s-%s-metrics", serviceName, environment));
            tracer = GlobalOpenTelemetry.getTracer(String.format("%s-%s-tracer", serviceName, environment));

            SERVICE_ATOMIC_REFERENCE.set(this);
        } else {
            throw new UnsupportedOperationException("Observability service is already initialized");
        }

    }

    /**
     * Retrieves the singleton instance of ObservabilityService.
     *
     * @return the singleton instance
     */
    public static ObservabilityService get() {
        return CommonUtil.getNonNullEntity(SERVICE_ATOMIC_REFERENCE, "Observability service is not initialized yet");
    }

    /**
     * Formats the given key by prefixing the service name.
     *
     * @param key the attribute key to format
     * @return the formatted key
     */
    public String formatKey(final String key) {
        Objects.requireNonNull(key, "Attribute key cannot be null");
        return String.format("%s.%s", serviceName, key);
    }

    /**
     * Retrieves the Meter instance for exporting metrics.
     *
     * @return the Meter instance
     */
    public Meter getMeter() {
        return meter;
    }

    /**
     * Retrieves the Tracer instance for exporting trace data.
     *
     * @return the Tracer instance
     */
    public Tracer getTracer() {
        return tracer;
    }


}

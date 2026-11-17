package com.tlc.observability.internal.aspect;

import com.tlc.observability.internal.ObservabilityService;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.atomic.AtomicReference;

import static com.tlc.observability.internal.aspect.MetricsAspect.LOGGER;
import static com.tlc.observability.internal.aspect.MetricsAspect.buildAttributes;

/**
 * Aspect for measuring the execution time of methods annotated with {@link com.tlc.observability.annotations.MeasureTime}.
 *
 * <p>
 * This aspect intercepts method calls that are annotated with {@code @MeasureTime} and measures their execution time.
 * </p>
 *
 * <b>Example usage:</b>
 * <pre>
 * {@code
 * @MeasureTime
 * public void myMethod() {
 *     // Method logic
 * }
 * }
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
@Aspect
public class MeasureTimeAspect {

    private static final AtomicReference<LongHistogram> HISTOGRAM = new AtomicReference<>();


    /**
     * Advice that runs around the execution of methods annotated with {@code @MeasureTime}.
     * It measures the execution time of the method and records it in the histogram.
     *
     * @param joinPoint provides contextual information about the intercepted method call.
     * @return the result of the method execution.
     * @throws Throwable if the method execution fails.
     */
    @Around("com.tlc.observability.internal.aspect.MetricsAspect.measureTime()")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            final Object result =  joinPoint.proceed();
            return result;
        } finally {
            final long endTime = System.currentTimeMillis();
            LOGGER.debug("called measure time {}", buildAttributes());
            getExecutionTimeHistogram().record((endTime - startTime), buildAttributes());
        }

    }

    /**
     * Lazily initializes and retrieves the {@link LongHistogram} for measuring method execution times.
     *
     * <p>
     * Ensures that the histogram is created only once and shared across all tracked methods. This process is
     * synchronized to prevent race conditions during initialization.
     * </p>
     *
     * @return the {@link LongHistogram} instance for tracking execution time metrics.
     */
    private LongHistogram getExecutionTimeHistogram() {
        LongHistogram histogram = HISTOGRAM.get();

        if (histogram == null) {
            synchronized (this) {
                histogram = HISTOGRAM.get();

                if (histogram == null) {
                    final Meter observabilityMeter = ObservabilityService.get().getMeter();

                    histogram = observabilityMeter.histogramBuilder("method_execution_time")
                            .setDescription("Tracks the execution time of methods annotated with @MeasureTime")
                            .setUnit("ms")
                            .ofLongs()
                            .build();
                    HISTOGRAM.set(histogram);
                }
            }
        }

        return histogram;
    }


}

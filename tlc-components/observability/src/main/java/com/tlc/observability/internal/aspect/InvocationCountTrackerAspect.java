package com.tlc.observability.internal.aspect;

import com.tlc.observability.internal.ObservabilityService;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.atomic.AtomicReference;

import static com.tlc.observability.internal.aspect.MetricsAspect.LOGGER;
import static com.tlc.observability.internal.aspect.MetricsAspect.buildAttributes;

/**
 * Aspect for tracking method invocation counts using OpenTelemetry metrics.
 *
 * <p>
 * This aspect uses the {@link com.tlc.observability.annotations.TrackInvocationCount} annotation to identify methods or constructors
 * that need invocation count tracking. It utilizes OpenTelemetry's {@link LongCounter} to increment
 * and record the count of method invocations.
 * </p>
 *
 * <p>
 * <b>Example:</b> Add the {@code @TrackInvocationCount} annotation to any method to start tracking its invocation count:
 * </p>
 *
 * <pre>
 * {@code
 * @TrackInvocationCount
 * public void myTrackedMethod() {
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
public class InvocationCountTrackerAspect {

    private static AtomicReference<LongCounter> COUNTER = new AtomicReference<>();

    /**
     * Advice that runs after the method execution to increment the invocation counter.
     *
     * @param joinPoint provides contextual information about the intercepted method call.
     */
    @After("com.tlc.observability.internal.aspect.MetricsAspect.trackInvocationCount()")
    public void countMethodInvocation(JoinPoint joinPoint) {
        LOGGER.debug("called invoke counter {}", buildAttributes());
        getCounter().add(1, buildAttributes());
    }

    /**
     * Lazily initializes and retrieves the {@link LongCounter} for tracking method invocations.
     *
     * <p>
     * Ensures that the counter is created only once and shared across all invocations. The creation
     * process is synchronized to prevent race conditions during initialization.
     * </p>
     *
     * @return the {@link LongCounter} instance for tracking invocation counts.
     */
    private LongCounter getCounter() {
        LongCounter counter = COUNTER.get();

        if (counter == null) {
            synchronized (this) {
                counter = COUNTER.get();

                if (counter == null) {
                    final Meter meter = ObservabilityService.get().getMeter();

                    counter = meter.counterBuilder("method_invocation_count")
                            .setDescription("Counts the number of times a method is invoked")
                            .setUnit("1")
                            .build();
                    COUNTER.set(counter);
                }
            }
        }

        return counter;
    }

}

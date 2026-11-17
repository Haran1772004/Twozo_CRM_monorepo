package com.tlc.observability.internal.aspect;

import com.tlc.observability.internal.ObservabilityService;
import com.tlc.observability.internal.context.InternalMetricsContext;
import com.tlc.observability.internal.context.MethodInfo;
import com.tlc.observability.internal.utils.MethodExtractor;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aspect for handling metrics related to methods annotated with {@link com.tlc.observability.annotations.MeasureTime}
 * and {@link com.tlc.observability.annotations.TrackInvocationCount}.
 * <p>
 * This class defines pointcuts and advice to handle the execution of methods with these annotations,
 * logging relevant metrics during the method execution. It tracks metrics such as execution time and
 * invocation count.
 * </p>
 *
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 */
@Aspect
public class MetricsAspect  {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MetricsAspect.class);

    /**
     * Pointcut definition for methods annotated with {@link com.tlc.observability.annotations.MeasureTime}.
     *
     * <p>
     * This pointcut targets methods that are annotated with {@code @MeasureTime} and matches any method signature
     * for execution.
     * </p>
     */
    @Pointcut("@annotation(com.tlc.observability.annotations.MeasureTime) && execution(* *(..))")
    public void measureTime() {

    }

    /**
     * Pointcut definition for methods annotated with {@link com.tlc.observability.annotations.TrackInvocationCount}.
     *
     * <p>
     * This pointcut targets any method or constructor that:
     * <ul>
     * <li>Is annotated with {@code @TrackInvocationCount}.</li>
     * <li>Matches the method signature pattern specified in the `execution` expression.</li>
     * </ul>
     * </p>
     */
    @Pointcut("@annotation(com.tlc.observability.annotations.TrackInvocationCount) && execution(* *(..))")
    public static void trackInvocationCount() {
    }

    /**
     * Around advice that applies to methods annotated with {@code @MeasureTime} or {@code @TrackInvocationCount}.
     * <p>
     * This advice runs before and after the execution of the annotated method, allowing metrics to be collected and cleaned up.
     * </p>
     *
     *
     * @param joinPoint The proceeding join point that represents the intercepted method invocation.
     * @return The result of the method execution.
     * @throws Throwable If any exception occurs during the method execution.
     */
    @Around("trackInvocationCount() || measureTime()")
    public Object handleMetricsAnnotations(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            InternalMetricsContext.init();

            final MethodInfo methodInfo = MethodExtractor.extractMethodInfo(joinPoint);

            InternalMetricsContext.add("package", methodInfo.packageName());
            InternalMetricsContext.add("className", methodInfo.className());
            InternalMetricsContext.add("methodName", methodInfo.name());

            return joinPoint.proceed();
        } finally {
            InternalMetricsContext.clear();
            LOGGER.debug("called metric context cleaner {}", buildAttributes());
        }
    }

    /**
     * Builds the attributes for the metrics from the {@link InternalMetricsContext} data.
     *
     * @return A set of attributes containing the method execution details.
     */
    protected static Attributes buildAttributes() {
        final AttributesBuilder attributesBuilder = Attributes.builder();
        final ObservabilityService observabilityService = ObservabilityService.get();

        InternalMetricsContext.getAttributes().ifPresent(context -> {
            context.forEach((key, value) -> {
                attributesBuilder.put(observabilityService.formatKey(key), String.valueOf(value));
            });

        });

        return attributesBuilder.build();
    }
}

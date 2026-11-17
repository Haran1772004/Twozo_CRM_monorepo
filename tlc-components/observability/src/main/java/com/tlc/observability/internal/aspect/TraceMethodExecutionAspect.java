package com.tlc.observability.internal.aspect;

import com.tlc.observability.annotations.TraceMethodExecution;
import com.tlc.observability.internal.ObservabilityService;
import com.tlc.observability.internal.context.InternalTracerContext;
import com.tlc.observability.internal.context.MethodInfo;
import com.tlc.observability.internal.utils.MethodExtractor;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Aspect for tracing method execution annotated with {@link TraceMethodExecution}.
 *
 * <p>
 * This aspect intercepts methods annotated with {@code @TraceMethodExecution}, creating spans for method execution
 * and capturing their execution context. It supports the creation of child spans and the addition of attributes
 * for tracing and observability.
 * </p>
 *
 * <b>Example usage:</b>
 * <pre>
 * {@code
 * @TraceMethodExecution(name = "myMethod", kind = SpanKind.SERVER)
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
public class TraceMethodExecutionAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceMethodExecutionAspect.class);

    /**
     * Pointcut definition for methods annotated with {@link TraceMethodExecution}.
     * This pointcut captures any method annotated with {@code @TraceMethodExecution}.
     */
    @Pointcut("@annotation(com.tlc.observability.annotations.TraceMethodExecution) && execution(* *(..))")
    public void traceMethodExecution() {

    }

    /**
     * Advice that runs around the execution of methods annotated with {@code @TraceMethodExecution}.
     * It creates and manages spans to trace the execution of the method.
     *
     * @param joinPoint provides contextual information about the intercepted method call.
     * @return the result of the method execution.
     * @throws Throwable if the method execution fails.
     */
    @Around("traceMethodExecution()")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final TraceMethodExecution tracePoint = signature.getMethod().getAnnotation(TraceMethodExecution.class);
        final String spanName = Objects.requireNonNull(tracePoint.name(), "Tracer name should never be null");

        final Context parentSpanContext = Context.current();
        final Span parentSpan = Span.current();

        try (final Scope parentContextScope = parentSpanContext.makeCurrent()) {
            final ObservabilityService observabilityService = ObservabilityService.get();
            final Tracer tracer = observabilityService.getTracer();
            final Span childSpan = tracer.spanBuilder(spanName)
                    .setSpanKind(getSpanKind(tracePoint))
                    .startSpan();

            LOGGER.debug("Start of child span {}", tracePoint.name());

            try (final Scope childSpanScope = childSpan.makeCurrent()) {
                final Context currentContext = InternalTracerContext.createNewContextForSpan("span.name", spanName);

                try (final Scope childContextScope = currentContext.makeCurrent()) {
                    final MethodInfo methodInfo = MethodExtractor.extractMethodInfo(joinPoint);

                    InternalTracerContext.get()
                            .add("package", methodInfo.packageName())
                            .add("className", methodInfo.className())
                            .add("methodName", methodInfo.name());

                    final Object executionResult = joinPoint.proceed();

                    if (tracePoint.dynamicParamsEnabled()) {
                        InternalTracerContext.getAttributes().ifPresent(context -> {
                            context.forEach((key, value) ->
                                    childSpan.setAttribute(observabilityService.formatKey(key), String.valueOf(value)));
                        });
                    }


                    return executionResult;
                }


            } catch (Throwable throwable) {

                if (!tracePoint.skipErrors()) {
                    childSpan.recordException(throwable);
                    childSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
                }

                throw throwable;
            } finally {
                LOGGER.debug("End of child span {}", tracePoint.name());
                childSpan.end();
            }
        }

    }

    /**
     * Helper method to convert the {@link SpanKind} from the annotation into an OpenTelemetry {@link SpanKind}.
     *
     * @param tracePoint the {@link TraceMethodExecution} annotation to extract the span kind from.
     * @return the corresponding {@link SpanKind} value.
     */
    private SpanKind getSpanKind(TraceMethodExecution tracePoint) {
        return switch (tracePoint.kind()) {
            case CLIENT -> SpanKind.CLIENT;
            case SERVER -> SpanKind.SERVER;
            case PRODUCER -> SpanKind.PRODUCER;
            case CONSUMER -> SpanKind.CONSUMER;
            default -> SpanKind.INTERNAL;
        };
    }
}


package com.tlc.observability.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to trace the execution of methods or constructors for observability.
 *
 * <p>
 * This annotation enables tracing of method or constructor executions, allowing
 * the generation of spans in distributed tracing systems like OpenTelemetry. The
 * annotation can be configured to specify the kind of operation being traced,
 * provide a custom name for the span, and optionally skip capturing errors.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * &#64;TraceMethodExecution(name = "process-data", kind = TraceMethodExecution.Kind.SERVER)
 * public void processData() {
 *     // method logic
 * }
 * </pre>
 *
 * <p>
 * The tracing logic is typically implemented using an aspect or interceptor
 * that handles annotated methods or constructors, creating spans accordingly.
 * </p>
 *
 * <h3>Configuration Options:</h3>
 * <ul>
 *   <li><strong>kind</strong>: Specifies the type of operation being traced, such as
 *   SERVER, CLIENT, PRODUCER, etc. Default is {@code INTERNAL}.</li>
 *   <li><strong>name</strong>: Defines a custom name for the span. This is a required field.</li>
 *   <li><strong>skipErrors</strong>: If {@code true}, errors encountered during the execution
 *   are not included in the span's metadata. Default is {@code false}.</li>
 * </ul>
 *
 * <p>
 * Retention policy: {@link RetentionPolicy#RUNTIME} ensures the annotation is available
 * at runtime for reflection-based tools or AOP frameworks. Target: {@link ElementType#METHOD}
 * and {@link ElementType#CONSTRUCTOR} allow usage on both methods and constructors.
 * </p>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface TraceMethodExecution {

    enum Kind {
        INTERNAL,
        SERVER,
        CLIENT,
        PRODUCER,
        CONSUMER
    }

    Kind kind() default Kind.INTERNAL;

    String name();

    boolean dynamicParamsEnabled() default true;
    boolean skipErrors() default false;

}

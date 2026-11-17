package com.tlc.observability.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to measure the execution time of a method.
 *
 * <p>
 * This annotation can be applied to methods to enable tracking
 * of their execution duration, typically for performance monitoring
 * and debugging purposes. The actual measurement logic should be
 * implemented using an aspect or a method interceptor, such as
 * with Spring AOP or other aspect-oriented programming frameworks.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * &#64;MeasureTime
 * public void someMethod() {
 *     // method logic
 * }
 * </pre>
 *
 * <p>
 * <b>Author:</b> Ashok<br>
 * <b>Version:</b> 1.0<br>
 * <b>Since:</b> 2024
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MeasureTime {
}

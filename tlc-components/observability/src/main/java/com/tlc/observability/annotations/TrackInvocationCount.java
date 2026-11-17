package com.tlc.observability.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to track the invocation count of a method.
 *
 * <p>
 * This annotation can be applied to methods to monitor how many times the
 * method has been invoked. It is commonly used for logging, debugging, or
 * gathering metrics about method usage.
 * </p>
 *
 * <p>
 * The implementation of tracking is typically handled by an aspect or
 * interceptor, which increments a counter each time the annotated method
 * is called.
 * </p>
 *
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * &#64;TrackInvocationCount
 * public void processRequest() {
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
public @interface TrackInvocationCount {
}

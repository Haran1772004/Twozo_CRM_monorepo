package com.tlc.web.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks an Action class with its API version.
 * </p>
 *
 * <p>
 * Multiple Action classes can share the same path and HTTP method
 * as long as they have different API versions. The version is resolved
 * from request headers at runtime.
 * </p>
 *
 * @version 1.0.
 * @author Ajay.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    /**
     * <p>
     * The version identifier for this Action.
     * </p>
     *
     * @return the version string.
     */
    String value();

    /**
     * <p>
     * Indicates whether this version is deprecated.
     * </p>
     *
     * @return whether the version is deprecated.
     */
    boolean deprecated() default false;

    /**
     * <p>
     * Optional message to include in deprecation warnings.
     * </p>
     *
     * @return deprecation message or empty string.
     */
    String deprecationMessage() default "";

    /**
     * <p>
     * The version to redirect to when this version is sunset.
     * </p>
     *
     * @return successor version string or empty.
     */
    String successor() default "";

    /**
     * <p>
     * Indicates whether this version is the default version for the path.
     * At most one version per path may be marked as default.
     * If no version is marked, version "1" is used as the fallback default.
     * </p>
     *
     * @return whether this is the default version.
     */
    boolean isDefaultVersion() default false;

    /**
     * <p>
     * The operation types this version handles.
     * </p>
     *
     * <p>
     * If non-empty, this version only handles the specified operation types.
     * Any request for a type not in this list will be delegated to the {@link #fallbackVersion()}.
     * If empty (default), this version handles all operation types.
     * </p>
     *
     * @return array of handled type names.
     */
    String[] handles() default {};

    /**
     * <p>
     * The version to fall back to for unhandled types when {@link #handles()} is non-empty.
     * </p>
     *
     * <p>
     * Resolution order: if set, redirects to this version; if empty, redirects to
     * the version marked with {@link #isDefaultVersion()}; if neither exists, falls back to "1".
     * </p>
     *
     * @return fallback version string, defaults to empty.
     */
    String fallbackVersion() default "";
}

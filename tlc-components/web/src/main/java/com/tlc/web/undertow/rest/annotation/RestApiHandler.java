package com.tlc.web.undertow.rest.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is used to register the rest controller actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestApiHandler {

    /**
     * The path mapping URIs &mdash; for example, {@code "/profile"}.
     */
    String path();
}

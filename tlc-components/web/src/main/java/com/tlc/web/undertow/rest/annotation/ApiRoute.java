package com.tlc.web.undertow.rest.annotation;

import com.tlc.web.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is used to route the rest web actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiRoute {

    String path() default "";

    HttpMethod method();
}

package com.tlc.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebAction
{
    String path();

    HttpMethod[] method() default {HttpMethod.POST};

    /**
     * Indicates whether the action is permitted to be blocking
     *
     * @return true if blocking operations are allowed for the action; false otherwise
     */
    boolean isBlockingOperation() default false;
}

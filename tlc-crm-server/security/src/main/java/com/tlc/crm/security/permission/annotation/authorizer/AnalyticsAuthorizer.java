package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.Action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation is represents the analytics module
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AnalyticsAuthorizer
{

    /**
     * <p>
     * Gets the action type.
     * </p>
     *
     * @return {@link Action.Type}
     */
    Action.Type action() default Action.Type.COMMON;
}

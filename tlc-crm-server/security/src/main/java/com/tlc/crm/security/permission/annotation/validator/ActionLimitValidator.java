package com.tlc.crm.security.permission.annotation.validator;

import com.tlc.crm.security.permission.model.LimitActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation denoting the checker for the limitation-related permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionLimitValidator {

    /**
     * Gets the {@link LimitActionType}.
     *
     * @return {@link LimitActionType}.
     */
    LimitActionType type();
}

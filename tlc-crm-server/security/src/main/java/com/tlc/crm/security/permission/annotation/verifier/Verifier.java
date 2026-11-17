package com.tlc.crm.security.permission.annotation.verifier;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation denoting the verifier for the permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Verifier {

    /**
     * Gets the {@link Class} of authorizer.
     *
     * @return {@link Class} of authorizer.
     */
    Class<? extends Annotation> authorizerClass();
}

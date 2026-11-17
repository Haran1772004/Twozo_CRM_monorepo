package com.tlc.crm.security.permission.annotation.authorizer;

import com.tlc.crm.security.permission.model.LimitActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * An annotation denoting the authorizer for the limit related permissions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LimitAuthorizer {

     /**
      * <p>
      * Provides the type of the limitation checker.
      * </p>
      *
      * @return Limitation checker type.
      */
     LimitActionType[] types();

     /**
      * <p>
      * Provides the API types which to be validated.
      * </p>
      *
      * @return API Types.
      */
     String[] apiTypes();
}

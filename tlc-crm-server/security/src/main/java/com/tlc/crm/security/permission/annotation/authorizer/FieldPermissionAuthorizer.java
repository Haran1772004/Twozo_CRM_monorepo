package com.tlc.crm.security.permission.annotation.authorizer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to enable field-level authorization for an API endpoint.
 *
 * @author Sathish
 * @version 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldPermissionAuthorizer {

    /**
     * The name of the module or entity source (e.g., "Deals", Contacts, Companies, Products) to validate permissions against.
     *
     * @return the source name.
     */
    String source() default "";
}
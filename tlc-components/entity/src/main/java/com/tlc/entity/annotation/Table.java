package com.tlc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define the table name for an entity class.
 * This annotation should be used at runtime and can be applied only to classes.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE } )
public @interface Table
{
    /**
     * Specifies the name of the database table associated with the entity class.
     *
     * @return the name of the database table
     */
    String name();
}
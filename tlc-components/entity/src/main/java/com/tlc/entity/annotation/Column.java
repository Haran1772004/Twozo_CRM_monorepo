package com.tlc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the column details for mapping a field to a database column.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD } )
public @interface Column
{
    /**
     * Specifies the name of the column in the database table.
     *
     * @return the name of the column
     */
    String name();

    /**
     * Specifies whether the column is a primary key or not.
     * Default value is false.
     *
     * @return true if the column is a primary key, false otherwise
     */
    boolean isPrimaryKey() default false;
}

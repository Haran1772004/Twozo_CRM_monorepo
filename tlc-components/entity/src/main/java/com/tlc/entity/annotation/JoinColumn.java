package com.tlc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the join column details for mapping a relationship.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD } )
public @interface JoinColumn
{
    /**
     * Specifies the local column name in the owning entity's table.
     * The Default value is an empty string.
     *
     * @return the local column name
     */
    String localColumn() default "ID";

    /**
     * Specifies the column name in the referenced entity's table.
     * This is a required attribute.
     *
     * @return the referenced column name
     */
    String referenceColumn();
}

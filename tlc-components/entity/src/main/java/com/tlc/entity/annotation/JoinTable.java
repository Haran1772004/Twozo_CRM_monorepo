package com.tlc.entity.annotation;

import com.tlc.entity.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the join table details for mapping a relationship.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.ANNOTATION_TYPE } )
public @interface JoinTable
{
    /**
     * Specifies the alias name to be used for the target entity in the join table.
     *
     * @return The alias name, or an empty string if not set.
     */
    String aliasName() default "";

    /**
     * Specifies the join columns associated with the join table.
     *
     * @return the join columns
     */
    JoinColumn[] joinColumns();

    /**
     * Specifies the entity class associated with the join table.
     *
     * @return the model class
     */
    Class<? extends Entity<?>> entityClass();
}

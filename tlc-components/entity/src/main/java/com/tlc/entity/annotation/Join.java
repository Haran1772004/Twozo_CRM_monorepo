package com.tlc.entity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a join relationship between entities.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.FIELD } )
public @interface Join
{

    /**
     * Specifies the type of relation, such as OneToOne, OneToMany, ManyToOne, or ManyToMany.
     *
     * @return The type of relation.
     */
    Relation relation();

    /**
     * Specifies the cascade operations to be applied to the relation.
     * Cascades determine how operations like "delete" or update are propagated from one entity to another.
     *
     * @return The cascade operations.
     */
    Cascade[] cascades() default Cascade.SELECT;

    /**
     * Specifies whether the relation is nullable or not.
     * If set to true, the field can hold null values; otherwise, it cannot.
     *
     * @return True if the relation is nullable, false otherwise.
     */
    boolean nullable() default false;

    /**
     * Specifies the join tables associated with the relation.
     * This is an array because there could be multiple join tables in some cases.
     *
     * @return The join tables associated with the relation.
     */
    JoinTable[] joinTables();
}
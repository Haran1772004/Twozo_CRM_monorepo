package com.tlc.entity.annotation;

/**
 * Enumeration for specifying relationship types.
 */
public enum Relation
{
    /**
     * One-to-one relationship.
     */
    ONE_TO_ONE,

    /**
     * Many-to-one relationship.
     */
    MANY_TO_ONE,

    /**
     * One-to-many relationship.
     */
    ONE_TO_MANY,

    /**
     * Many-to-many relationship.
     */
    MANY_TO_MANY
}
package com.tlc.entity.annotation;

/**
 * Enumeration for specifying cascade types.
 */
public enum Cascade
{
    /**
     * Cascade select operation.
     */
    SELECT,

    /**
     * Cascade persist (insert/update) operation.
     */
    PERSIST,

    /**
     * Cascade delete operation.
     */
    DELETE,

    /**
     * Cascade all operations (select, persist, delete).
     */
    ALL,
}

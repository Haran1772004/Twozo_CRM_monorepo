package com.tlc.sql.common.api.meta;

/**
 * Defines the primary key metadata for a database table.
 * Provides access to the primary key column name and its sequence generator, if available.
 */
public interface PKDefinition {

    /**
     * Returns the name of the primary key column.
     *
     * @return the primary key column name
     */
    String getColumnName();

    /**
     * Checks whether this primary key has an associated sequence generator
     * for auto-generating key values.
     *
     * @return {@code true} if a sequence generator is available, {@code false} otherwise
     */
    boolean hasSequenceGenerator();

    /**
     * Returns the next value from the associated sequence generator.
     *
     * @return the next sequence value
     * @throws com.tlc.commons.code.impl.DefaultErrorCode if no sequence generator is available
     */
    long getNextSequence();
}
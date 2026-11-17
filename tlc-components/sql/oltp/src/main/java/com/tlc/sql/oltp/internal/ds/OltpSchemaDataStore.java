package com.tlc.sql.oltp.internal.ds;

import com.tlc.sql.common.api.ds.SchemaDataStore;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.handler.OltpDDLHandler;

/**
 * OLTP-specific extension of {@link SchemaDataStore} that adds
 * sequence management operations for PostgreSQL databases.
 */
public interface OltpSchemaDataStore extends SchemaDataStore<OltpColumnDefinition, OltpTableDefinition, OltpDDLHandler> {

    /**
     * Initializes an existing database sequence with the given start value and increment.
     *
     * @param sequenceName the name of the sequence to initialize
     * @param start        the starting value for the sequence
     * @param incrementBy  the increment step for the sequence
     */
    void initializeSequence(String sequenceName, int start, int incrementBy);

    /**
     * Creates a new database sequence with the given start value and increment.
     *
     * @param sequenceName the name of the sequence to create
     * @param start        the starting value for the sequence
     * @param incrementBy  the increment step for the sequence
     */
    void createSequence(String sequenceName, int start, int incrementBy);

    /**
     * Updates the increment step of an existing database sequence.
     *
     * @param sequenceName the name of the sequence to update
     * @param incrementBy  the new increment step for the sequence
     */
    void updateSequence(String sequenceName, int incrementBy);

}
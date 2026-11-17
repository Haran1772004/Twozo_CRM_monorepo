package com.tlc.sql.oltp.api.meta;

import com.tlc.sql.common.api.meta.PKDefinition;import com.tlc.sql.common.api.meta.SequenceDefinition;

import java.util.Objects;

/**
 * OLTP (PostgreSQL) implementation of {@link PKDefinition}.
 * Supports optional sequence-based primary key generation via {@link SequenceDefinition}.
 *
 * @author Abishek
 * @version 1.0
 */
public class OltpPKDefinition implements PKDefinition {

    private final String keyName;
    private final String columnName;
    private final SequenceDefinition sequenceDefinition;

    public OltpPKDefinition(String keyName, String columnName) {
        this(keyName, columnName, null);
    }

    public OltpPKDefinition(String keyName, String columnName, SequenceDefinition sequenceDefinition) {
        this.keyName = Objects.requireNonNull(keyName);
        this.columnName = Objects.requireNonNull(columnName);
        this.sequenceDefinition = sequenceDefinition;
    }

    public String getKeyName() {
        return keyName;
    }

    public SequenceDefinition getSequenceDefinition() {
        return sequenceDefinition;
    }

    /**
     * {@inheritDoc}
     */
    public long getNextSequence() {
        return sequenceDefinition.getSequenceGenerator().getNextNumber();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasSequenceGenerator() {
        return sequenceDefinition != null;
    }

    @Override
    public String toString() {
        return keyName + "=" + columnName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName() {
        return columnName;
    }}
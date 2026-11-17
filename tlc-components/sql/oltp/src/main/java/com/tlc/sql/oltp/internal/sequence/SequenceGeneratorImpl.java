package com.tlc.sql.oltp.internal.sequence;

import com.tlc.sql.common.api.sequence.SequenceGenerator;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;

import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class SequenceGeneratorImpl implements SequenceGenerator
{
    private final String sequenceName;
    protected final OltpAdminDataStore dataStore;

    public SequenceGeneratorImpl(final String sequenceName, final OltpAdminDataStore dataStore)
    {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.sequenceName = Objects.requireNonNull(sequenceName);
        @SuppressWarnings("unused")
        final long nextNumber = getNextNumber();
    }

    @Override
    public long getNextNumber()
    {
        return dataStore.getSequenceNextValue(sequenceName);
    }
}

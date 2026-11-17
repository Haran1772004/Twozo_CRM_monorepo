package com.tlc.sql.oltp.internal.sequence;

import com.tlc.sql.common.api.sequence.SequenceGenerator;
import com.tlc.sql.oltp.internal.ds.OltpAdminDataStore;
import org.javatuples.Pair;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author Abishek
 * @version 1.0
 */
public class BatchSeqGeneratorImpl implements SequenceGenerator
{
    private final String sequenceName;

    private final AtomicLong seqNumber = new AtomicLong();
    private final AtomicLong seriesEnd = new AtomicLong();

    protected final OltpAdminDataStore dataStore;
    private final Lock sync = new ReentrantLock();
    public BatchSeqGeneratorImpl(final String sequenceName, final OltpAdminDataStore dataStore)
    {
        this.dataStore = Objects.requireNonNull(dataStore);
        this.sequenceName = Objects.requireNonNull(sequenceName);
        loadSeries();
    }


    @Override
    public long getNextNumber()
    {
        sync.lock();
        try
        {
            final long newNumber = seqNumber.getAndIncrement();
            if(newNumber == seriesEnd.get())
            {
                loadSeries();
            }
            return newNumber;
        }
        finally
        {
            sync.unlock();
        }
    }

    private void loadSeries()
    {
        final Pair<Long, Long> newStart = dataStore.getSequenceNextAndLimitValue(sequenceName);
        final long end = newStart.getValue0();
        seriesEnd.set(end);
        seqNumber.set(end - newStart.getValue1() + 1);
    }
}

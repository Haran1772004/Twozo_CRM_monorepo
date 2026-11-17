package com.tlc.email.common.model;

import com.tlc.email.common.codec.ResponseCodec;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.exchange.EmailData;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link ThreadHolder}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailData
 */
public class ThreadHolder extends EmailData implements ResponseCodec {

    private EmailAddress syncedEmailAddress;
    private Collection<Thread> threads;

    public EmailAddress getSyncedEmailAddress() {
        return syncedEmailAddress;
    }

    public void setSyncedEmailAddress(final EmailAddress syncedEmailAddress) {
        this.syncedEmailAddress = syncedEmailAddress;
    }

    public Collection<Thread> getThreads() {
        return threads;
    }

    public void setThreads(final Collection<Thread> threads) {
        this.threads = threads;
    }
}

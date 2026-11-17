package com.tlc.crm.notifications.api.call.model;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents a collection of call history records.
 * <p>
 * This class maintains a list of {@link CallDetails} and provides
 * methods to add and retrieve call records. It implements {@link Iterable}
 * to allow iteration over stored call records.
 * </p>
 *
 * <p><b>Concurrency Note:</b> The iterator checks for concurrent modification
 * to prevent inconsistencies during iteration.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CallHistory implements Iterable<CallDetails> {

    private final List<CallDetails> callDetailsList;

    public CallHistory() {
        this.callDetailsList = new ArrayList<>();
    }

    public void addCallDetails(final CallDetails callDetails) {
        callDetailsList.add(callDetails);
    }

    public CallDetails getCallDetails(final int index) {
        return callDetailsList.get(index);
    }

    /**
     * Returns an iterator over elements of type {@code CallDetails}.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<CallDetails> iterator() {
        return new Iterator<>() {

            private final int size = callDetailsList.size();
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public CallDetails next() {
                if (cursor >= size) {
                    throw new NoSuchElementException("No more CallDetails in CallHistory.");
                } else if (size != callDetailsList.size()) {
                    throw new ConcurrentModificationException("CallHistory modified during iteration.");
                } else {
                    return callDetailsList.get(cursor++);
                }
            }
        };
    }
}

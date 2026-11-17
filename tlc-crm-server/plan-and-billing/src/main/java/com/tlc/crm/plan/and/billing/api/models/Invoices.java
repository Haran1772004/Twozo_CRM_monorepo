package com.tlc.crm.plan.and.billing.api.models;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Aggregates multiple Invoice objects for an organization, supporting bulk operations and iteration over invoices.
 *
 * @author Sathish
 * @version 1.0
 */
public class Invoices implements Iterable<Invoice> {

    private final List<Invoice> invoices;

    public Invoices(final Collection<Invoice> invoices) {
        this.invoices = new ArrayList<>(invoices);
    }

    public Invoices() {
        this.invoices = new ArrayList<>();
    }

    public void add(final Invoice invoice) {
        invoices.add(invoice);
    }

    public void addAll(final Collection<Invoice> invoices) {
        Objects.requireNonNull(invoices);
        this.invoices.addAll(invoices);
    }

    /**
     * Returns an iterator over elements of type {@link Invoice}.
     *
     * @return An iterator over the stored resources.
     */
    @NotNull
    @Override
    public Iterator<Invoice> iterator() {
        return new Iterator<>() {

            private final int size = invoices.size();
            private int cursor;

            /**
             * Checks if there are more elements to iterate over.
             *
             * @return {@code true} if more elements are available, {@code false} otherwise.
             */
            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            /**
             * Retrieves the next element in the iteration.
             *
             * @return The next {@link Invoice}.
             * @throws NoSuchElementException If no more elements exist.
             * @throws ConcurrentModificationException If the resource list is modified during iteration.
             */
            @Override
            public Invoice next() {
                if (cursor >= size) {
                    throw new NoSuchElementException("No more elements in Invoices.");
                } else if (size != invoices.size()) {
                    throw new ConcurrentModificationException("Invoices modified during iteration.");
                } else {
                    return invoices.get(cursor++);
                }
            }
        };
    }
}

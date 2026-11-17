package com.tlc.commons;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe counter providing atomic increment and decrement operations.
 *
 * @author Sathish
 * @version 1.0
 */
public final class AtomicCounter extends Counter {

    private final AtomicInteger count;

    public AtomicCounter() {
        this.count = new AtomicInteger(0);
    }

    public AtomicCounter(final int count) {
        this.count = new AtomicInteger(count);
    }

    public int getAndIncrement() {
        return count.getAndIncrement();
    }

    public int getAndDecrement() {
        return count.getAndDecrement();
    }

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    public int decrementAndGet() {
        return count.decrementAndGet();
    }

    public void increment() {
        count.incrementAndGet();
    }

    public void decrement() {
        count.decrementAndGet();
    }

    public int getCount() {
        return count.get();
    }

    public void setCount(final int count) {
        this.count.set(count);
    }

    public void increment(final int count) {
        this.count.addAndGet(count);
    }
}

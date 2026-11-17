package com.tlc.commons;

/**
 * Counter to get the increment and decrement count
 *
 * @author KarthickV
 * @version 1.0
 */
public class Counter {

    private int count;

    public Counter() {
        count = 0;
    }

    public Counter(final int count) {
        this.count = count;
    }

    public int getAndIncrement() {
        return count++;
    }

    public int getAndDecrement() {
        return count--;
    }

    public int incrementAndGet() {
        return ++count;
    }

    public int decrementAndGet() {
        return --count;
    }

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public int getCount() {
        return count;
    }

    public void setCount(final int count) {
        this.count = count;
    }

    public void increment(final int count) {
        this.count += count;
    }
}

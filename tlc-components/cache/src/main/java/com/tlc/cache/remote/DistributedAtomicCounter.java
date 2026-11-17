package com.tlc.cache.remote;

/**
 * Defines an abstraction for atomic numeric operations on various counters.
 * <p>Implementations are expected to ensure thread-safe, distributed atomicity.
 *
 * @author ThalaimalaiPandiyanT
 * @since 1.0
 */
public interface DistributedAtomicCounter {

    /**
     * Atomically decrements the current value by one and returns the previous value.
     *
     * @return The previous value before decrement.
     */
    long getAndDecrement();

    /**
     * Atomically adds the specified value to the current value and returns the updated result.
     *
     * @param value The value to add.
     * @return The updated value after addition.
     */
    long addAndGet(final long value);

    /**
     * Atomically sets the value to the given updated value if the current value equals the expected value.
     *
     * @param expect The expected current value.
     * @param update The new value to set if comparison succeeds.
     * @return {@code true} if the update occurred, {@code false} otherwise.
     */
    boolean compareAndSet(final long expect, final long update);

    /**
     * Atomically decrements the current value by one and returns the updated value.
     *
     * @return The updated value after decrement.
     */
    long decrementAndGet();

    /**
     * Retrieves the current value atomically without modification.
     *
     * @return The current value.
     */
    long get();

    /**
     * Atomically retrieves the current value and deletes the key.
     *
     * @return The value before deletion.
     */
    long getAndDelete();

    /**
     * Atomically adds the specified value to the current value and returns the previous value.
     *
     * @param value The value to add.
     * @return The previous value before addition.
     */
    long getAndAdd(final long value);

    /**
     * Atomically sets the current value to the specified value and returns the previous value.
     *
     * @param value The new value to set.
     * @return The previous value before update.
     */
    long getAndSet(final long value);

    /**
     * Atomically increments the current value by one and returns the updated value.
     *
     * @return The updated value after increment.
     */
    long incrementAndGet();

    /**
     * Atomically retrieves the current value and then increments it by one.
     *
     * @return The value before increment.
     */
    long getAndIncrement();

    /**
     * Sets the current value unconditionally.
     *
     * @param value The value to set.
     */
    void set(final long value);

    /**
     * Conditionally sets the value only if the current value is less than the specified threshold.
     *
     * @param less  The threshold to compare against.
     * @param value The new value to set if the current value is less.
     * @return {@code true} if the value was updated, {@code false} otherwise.
     */
    boolean setIfLess(final long less, final long value);

    /**
     * Conditionally sets the value only if the current value is greater than the specified threshold.
     *
     * @param greater The threshold to compare against.
     * @param value   The new value to set if the current value is greater.
     * @return {@code true} if the value was updated, {@code false} otherwise.
     */
    boolean setIfGreater(final long greater, final long value);

    /**
     * Deletes the key.
     *
     * @return true if it was exist and deleted else false.
     */
    boolean delete();
}

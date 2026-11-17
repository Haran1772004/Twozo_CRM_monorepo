package com.tlc.cache.internal.redis;

import com.tlc.cache.remote.DistributedAtomicCounter;

import org.redisson.api.RAtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of {@link DistributedAtomicCounter} that wraps a
 * Redis-backed {@link RAtomicLong} for performing distributed atomic operations.
 *
 * @author ThalaimalaiPandiyanT
 * @see DistributedAtomicCounter
 * @see RAtomicLong
 * @since 1.0
 */
final class RedisDistributedCounter implements DistributedAtomicCounter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisDistributedCounter.class);

    private final RAtomicLong rAtomicLong;

    RedisDistributedCounter(final RAtomicLong rAtomicLong) {
        this.rAtomicLong = rAtomicLong;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAndDecrement() {
        return rAtomicLong.getAndDecrement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long addAndGet(final long value) {
        return rAtomicLong.addAndGet(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compareAndSet(final long expect, final long update) {
        return rAtomicLong.compareAndSet(expect, update);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long decrementAndGet() {
        return rAtomicLong.decrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long get() {
        return rAtomicLong.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAndDelete() {
        return rAtomicLong.getAndDelete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAndAdd(final long value) {
        return rAtomicLong.getAndAdd(value);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public long getAndSet(final long value) {
        return rAtomicLong.getAndSet(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long incrementAndGet() {
        return rAtomicLong.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAndIncrement() {
        return rAtomicLong.getAndIncrement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(final long value) {
        rAtomicLong.set(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setIfLess(final long less, long value) {
        return rAtomicLong.setIfLess(less, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setIfGreater(final long greater, final long value) {
        return rAtomicLong.setIfGreater(greater, value);
    }

    /**
     * {@inheritDoc}
     *
     * @return true if it was exist and deleted else false.
     */
    @Override
    public boolean delete() {
        return rAtomicLong.delete();
    }
}

package com.tlc.scheduler.algorithm.roundrobin.service;

import com.tlc.cache.remote.DistributedAtomicCounter;
import com.tlc.cache.remote.CacheService;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinRule;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinTracker;

import org.redisson.api.RAtomicLong;

/**
 * Implements a rule-based round-robin strategy for task or resource distribution.
 * <p>
 * This strategy uses a Redis-backed {@code RAtomicLong} counter to maintain
 * atomic and distributed index progression per rule and organization.
 * Each rule has its own sequence, ensuring fair cyclic assignment
 * even across multiple JVM instances.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see RoundRobinRule
 * @see RoundRobinTracker
 * @see RAtomicLong
 */
public final class RuleBasedRoundRobinStrategy implements RoundRobinStrategy {

    private final CacheService cacheService;

    private RuleBasedRoundRobinStrategy() {
        this.cacheService = CacheServiceProvider.get();
    }

    private static final class Instance {
        private static final RuleBasedRoundRobinStrategy ROUND_ROBIN_STRATEGY = new RuleBasedRoundRobinStrategy();
    }

    public static RuleBasedRoundRobinStrategy getInstance() {
        return Instance.ROUND_ROBIN_STRATEGY;
    }

    /**
     * {@inheritDoc}
     *
     * @param roundRobinTracker The tracker object.
     * @return The next index position to assign, starting from {@code 0}.
     */
    @Override
    public int getIndex(final RoundRobinTracker roundRobinTracker) {
        final RoundRobinRule roundRobinRule = roundRobinTracker.getRoundRobinRule();
        final String key = String.format("%s:%d:%d", roundRobinTracker.getRoundRobinGroup().name(), roundRobinTracker.orgId(), roundRobinRule.getRuleEntityId());
        final DistributedAtomicCounter distributedAtomicCounter = cacheService.getOrInitAtomicCounter(key);
        long index = distributedAtomicCounter.getAndIncrement();

        return Math.floorMod(index, roundRobinTracker.getTotalCount());
    }

    /**
     * Resets the index of the round-robin sequence for the given key.
     *
     * @param key The unique Redis key representing the counter.
     */
    public void resetIndex(final String key) {
        final DistributedAtomicCounter distributedAtomicCounter = cacheService.getOrInitAtomicCounter(key);

        distributedAtomicCounter.set(0);
    }

    /**
     * Deletes the atomic key.
     *
     * @param key The unique Redis key representing the counter.
     */
    public void delete(final String key) {
        final DistributedAtomicCounter distributedAtomicCounter = cacheService.getOrInitAtomicCounter(key);

        distributedAtomicCounter.delete();
    }
}

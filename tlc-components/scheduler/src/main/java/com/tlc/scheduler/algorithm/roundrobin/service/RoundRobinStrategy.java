package com.tlc.scheduler.algorithm.roundrobin.service;

import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinTracker;

/**
 * Interface for round-robin strategy for task or resource distribution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RoundRobinStrategy {

    /**
     * Computes the next index in the round-robin sequence for the given {@link RoundRobinTracker}.
     *
     * @param roundRobinTracker The tracker object.
     * @return The next index position to assign, starting from {@code 0}.
     */
    int getIndex(final RoundRobinTracker roundRobinTracker);
}

package com.tlc.scheduler.algorithm.roundrobin.service;

import com.tlc.commons.code.ErrorCode;

import com.tlc.scheduler.internal.resource.SchedulerErrorCodes;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinTracker;

/**
 * Abstract base class for round-robin scheduling services.
 * Subclasses typically implement the specific scheduling behavior,
 * persistence, and limit enforcement.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class RoundRobinService {

    private final RuleBasedRoundRobinStrategy ruleBasedRoundRobinStrategy;

    protected RoundRobinService() {
        this.ruleBasedRoundRobinStrategy = RuleBasedRoundRobinStrategy.getInstance();
    }

    /**
     * Calculates the next index position to assign based on the given {@link RoundRobinTracker}.
     *
     * @param roundRobinTracker The tracker maintaining the current state of round-robin assignment.
     * @return The index representing the next assignee position.
     */
    protected final int getIndex(final RoundRobinTracker roundRobinTracker) {
        return switch (roundRobinTracker.getRoundRobinGroup().limitType()) {
            case RULE_BASED -> ruleBasedRoundRobinStrategy.getIndex(roundRobinTracker);
            default -> throw ErrorCode.get(SchedulerErrorCodes.INVALID_ROUND_ROBIN_LIMIT_TYPE);
        };
    }
}

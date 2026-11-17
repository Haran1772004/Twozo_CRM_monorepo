package com.tlc.scheduler.algorithm.roundrobin.model;

import com.tlc.validator.TlcModel;

/**
 * Represents a round-robin tracker configuration within the scheduling system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RoundRobinTracker implements TlcModel {

    private Long id;
    private Long orgId;
    private RoundRobinGroup roundRobinGroup;
    private RoundRobinRule roundRobinRule;
    private int currentIndex;
    private Long updatedAt;
    private Long lastResetAt;
    private ResetInterval resetInterval;
    private int totalCount;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setRoundRobinGroup(final RoundRobinGroup roundRobinGroup) {
        this.roundRobinGroup = roundRobinGroup;
    }

    public void setRoundRobinRule(final RoundRobinRule roundRobinRule) {
        this.roundRobinRule = roundRobinRule;
    }

    public void setCurrentIndex(final int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void setUpdatedAt(final Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastResetAt(final Long lastResetAt) {
        this.lastResetAt = lastResetAt;
    }

    public void setResetInterval(final ResetInterval resetInterval) {
        this.resetInterval = resetInterval;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public RoundRobinGroup getRoundRobinGroup() {
        return roundRobinGroup;
    }

    public RoundRobinRule getRoundRobinRule() {
        return roundRobinRule;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public Long getLastResetAt() {
        return lastResetAt;
    }

    public ResetInterval getResetInterval() {
        return resetInterval;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

}

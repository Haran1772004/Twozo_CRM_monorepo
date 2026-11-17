package com.tlc.scheduler.algorithm.roundrobin.model;

import com.tlc.validator.TlcModel;

/**
 * Represents a round-robin rule configuration within the scheduling system.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RoundRobinRule implements TlcModel {

    private Long id;
    private Long orgId;
    private Long ruleEntityId;
    private int lastAssignedIndex;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setRuleEntityId(final Long ruleEntityId) {
        this.ruleEntityId = ruleEntityId;
    }

    public void setLastAssignedIndex(final int lastAssignedIndex) {
        this.lastAssignedIndex = lastAssignedIndex;
    }

    public Long getRuleEntityId() {
        return ruleEntityId;
    }

    public int getLastAssignedIndex() {
        return lastAssignedIndex;
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

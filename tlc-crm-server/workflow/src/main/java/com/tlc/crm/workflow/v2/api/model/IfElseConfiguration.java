package com.tlc.crm.workflow.v2.api.model;

import jakarta.validation.Valid;

/**
 * If/else branch configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class IfElseConfiguration implements NodeConfiguration {

    @Valid
    private ConditionConfiguration condition;

    @Valid
    private NodeEvent nodeEvent;

    public NodeEvent getNodeEvent() {
        return nodeEvent;
    }

    public void setNodeEvent(final NodeEvent nodeEvent) {
        this.nodeEvent = nodeEvent;
    }

    public void setCondition(final ConditionConfiguration condition) {
        this.condition = condition;
    }

    public ConditionConfiguration getCondition() {
        return condition;
    }
}

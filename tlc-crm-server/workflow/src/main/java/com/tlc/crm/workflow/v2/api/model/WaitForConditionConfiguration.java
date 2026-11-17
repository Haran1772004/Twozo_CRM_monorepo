package com.tlc.crm.workflow.v2.api.model;

import jakarta.validation.Valid;

/**
 * Wait-for-condition node configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WaitForConditionConfiguration implements NodeConfiguration {

    @Valid
    private ConditionConfiguration condition;

    @Valid
    private SpanConfiguration span;

    @Valid
    private NodeEvent nodeEvent;

    public NodeEvent getNodeEvent() {
        return nodeEvent;
    }

    public void setNodeEvent(final NodeEvent nodeEvent) {
        this.nodeEvent = nodeEvent;
    }

    public ConditionConfiguration getCondition() {
        return condition;
    }

    public void setCondition(final ConditionConfiguration condition) {
        this.condition = condition;
    }

    public SpanConfiguration getSpan() {
        return span;
    }

    public void setSpan(final SpanConfiguration span) {
        this.span = span;
    }
}

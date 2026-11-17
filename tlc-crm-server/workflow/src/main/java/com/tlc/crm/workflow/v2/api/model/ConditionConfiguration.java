package com.tlc.crm.workflow.v2.api.model;

import com.tlc.crm.workflow.api.model.WorkflowCriteriaSet;

import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/**
 * Condition node configuration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class ConditionConfiguration implements NodeConfiguration {

    @Size(max = 2, groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.criteria.set.size.exceeds")
    @Valid
    private Collection<WorkflowCriteriaSet> criteriaSets;

    @Valid
    private NodeEvent nodeEvent;

    public Collection<WorkflowCriteriaSet> getCriteriaSets() {
        return criteriaSets;
    }

    public void setCriteriaSets(final Collection<WorkflowCriteriaSet> criteriaSets) {
        this.criteriaSets = criteriaSets;
    }

    public NodeEvent getNodeEvent() {
        return nodeEvent;
    }

    public void setNodeEvent(final NodeEvent nodeEvent) {
        this.nodeEvent = nodeEvent;
    }

}

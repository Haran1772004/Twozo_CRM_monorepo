package com.tlc.crm.workflow.v2.api.model;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * Directed edge between workflow V2 nodes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowNodeEdge {

    private Long id;
    private Long fromNodeId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.node.edge.type.required")
    private EdgeType edgeType;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.node.edge.required")
    private Long toNodeId;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(final Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public EdgeType getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(final EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public Long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(final Long toNodeId) {
        this.toNodeId = toNodeId;
    }

}
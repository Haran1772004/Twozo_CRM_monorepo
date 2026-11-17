package com.tlc.crm.workflow.v2.api.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * Node event for workflow nodes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class NodeEvent implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    private Long sourceNodeId;

    private Long nodeId;

    @NotNull(groups = {Group.Create.class}, message = "i18n.workflow.node.event.key.required")
    private String sourceNodeClientKey;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.workflow.node.event.type.required")
    private EventContextType contextType;

    public Long getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(final Long sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public String getSourceNodeClientKey() {
        return sourceNodeClientKey;
    }

    public void setSourceNodeClientKey(final String sourceNodeClientKey) {
        this.sourceNodeClientKey = sourceNodeClientKey;
    }

    public EventContextType getContextType() {
        return contextType;
    }

    public void setContextType(final EventContextType contextType) {
        this.contextType = contextType;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Long nodeId) {
        this.nodeId = nodeId;
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

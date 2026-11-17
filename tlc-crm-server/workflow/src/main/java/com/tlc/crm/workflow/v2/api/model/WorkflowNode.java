package com.tlc.crm.workflow.v2.api.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single node in a workflow V2 graph.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class WorkflowNode implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.node.client.key.required")
    private String clientKey;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.workflow.node.type.required")
    private NodeType nodeType;

    @Valid
    private NodeConfiguration configuration;

    @Valid
    private WorkflowNodeEdge nextEdge;

    @Valid
    private WorkflowNodeEdge fallbackEdge;

    private Long versionId;

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(final Long versionId) {
        this.versionId = versionId;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(final String clientKey) {
        this.clientKey = clientKey;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(final NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public NodeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final NodeConfiguration configuration) {
        this.configuration = configuration;
    }

    public static Map<String, WorkflowNode> newNodeMap() {
        return new LinkedHashMap<>();
    }

    public WorkflowNodeEdge getNextEdge() {
        return nextEdge;
    }

    public void setNextEdge(final WorkflowNodeEdge nextEdge) {
        this.nextEdge = nextEdge;
    }

    public WorkflowNodeEdge getFallbackEdge() {
        return fallbackEdge;
    }

    public void setFallbackEdge(final WorkflowNodeEdge fallbackEdge) {
        this.fallbackEdge = fallbackEdge;
    }

    public <T extends NodeConfiguration> T configurationAs(final Class<T> type) {
        final NodeConfiguration configuration = this.getConfiguration();

        return type.isInstance(configuration) ? type.cast(configuration) : null;
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

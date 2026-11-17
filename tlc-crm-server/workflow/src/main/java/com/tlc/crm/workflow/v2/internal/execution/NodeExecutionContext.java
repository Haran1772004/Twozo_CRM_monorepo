package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds live node walk state for one V2 execution.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public final class NodeExecutionContext {

    private final Long orgId;
    private final Long userId;
    private final boolean needNestedTrigger;
    private final JsonObject nestedTriggerInfo;
    private final WorkflowV2 workflow;
    private final Long executionId;
    private final Map<Long, WorkflowNode> nodesById;
    private final Pair<TlcModel, TlcModel> triggerModelPair;
    private final Map<Long, User> groupedUsers;
    private final Pair<TriggerSource, TlcModel> triggerSourceModel;
    private Long nodeId;
    private final Map<Short, Long> nodeIdToClientId;
    private Map<Long, Pair<String, TlcModel>> actionModels = new HashMap<>();

    public NodeExecutionContext(final Long orgId, final Long userId, final Pair<TlcModel, TlcModel> triggerModelPair,
                                final boolean needNestedTrigger, final JsonObject nestedTriggerInfo,
                                final WorkflowV2 workflow, final Long executionId,
                                final Map<Long, User> groupedUsers, final Pair<TriggerSource, TlcModel> triggerSourceModel) {
        this.orgId = orgId;
        this.userId = userId;
        this.triggerModelPair = triggerModelPair;
        this.workflow = workflow;
        this.executionId = executionId;
        this.needNestedTrigger = needNestedTrigger;
        this.nestedTriggerInfo = nestedTriggerInfo;
        this.nodesById = new HashMap<>();
        this.nodeIdToClientId = new HashMap<>();
        this.groupedUsers = groupedUsers;
        this.triggerSourceModel = triggerSourceModel;

        workflow.getNodesAsCollection().forEach(node -> {
            if (Objects.nonNull(node.id())) {
                nodesById.put(node.id(), node);
            }
        });

        workflow.getIdToClientKey().forEach((nodeId, clientKey) ->
                nodeIdToClientId.put(Short.valueOf(clientKey), nodeId));
    }

    public Long orgId() {
        return orgId;
    }

    public Long userId() {
        return userId;
    }

    public boolean isNeedNestedTrigger() {
        return needNestedTrigger;
    }

    public JsonObject nestedTriggerInfo() {
        return nestedTriggerInfo;
    }

    public WorkflowV2 workflow() {
        return workflow;
    }

    public Long executionId() {
        return executionId;
    }

    public Map<Long, WorkflowNode> nodesById() {
        return nodesById;
    }

    public Pair<TlcModel, TlcModel> triggerModelPair() {
        return triggerModelPair;
    }

    public Map<Long, User> groupedUsers() {
        return groupedUsers;
    }

    public Pair<TriggerSource, TlcModel> triggerSourceModel() {
        return triggerSourceModel;
    }

    public WorkflowNode node(final Long nodeId) {
        return nodesById.get(nodeId);
    }

    public Long nodeKey(final Short clientKey) {
        return nodeIdToClientId.get(clientKey);
    }

    public void addActionModel(final Long nodeId, final Pair<String, TlcModel> actionModel) {
        this.actionModels = this.actionModels == null ? new HashMap<>() : this.actionModels;

        this.actionModels.put(nodeId, actionModel);
    }

    public Map<Long, Pair<String, TlcModel>> getActionModels() {
        return actionModels;
    }

    public Pair<String, TlcModel> actionModelPair(final Long nodeId) {
        return actionModels.get(nodeId);
    }

    public void setNodeId(final Long nodeId) {
        this.nodeId = nodeId;
    }

    public Long nodeId() {
        return nodeId;
    }

    public WorkflowNode triggerNode() {
        return workflow.getNodesAsCollection().stream()
                .filter(node -> NodeType.TRIGGER == node.getNodeType())
                .findFirst()
                .orElse(null);
    }
}

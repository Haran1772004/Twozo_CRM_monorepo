package com.tlc.crm.workflow.v2.internal.execution;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.core.workflow.handler.TriggerModelHandler;

import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionDataParserFields;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2ExecutionManager;
import com.tlc.crm.workflow.v2.api.manager.WorkflowV2Manager;
import com.tlc.crm.workflow.v2.api.model.ExecutionEntity;
import com.tlc.crm.workflow.v2.api.model.NodeType;
import com.tlc.crm.workflow.v2.api.model.WorkflowNode;
import com.tlc.crm.workflow.v2.api.model.WorkflowV2;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;

import com.tlc.iam.common.models.User;

import com.tlc.validator.TlcModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes and deserializes {@link NodeExecutionContext} to JSON snapshots.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class NodeExecutionContextService {

    private final WorkflowV2Manager workflowV2Manager;
    private final WorkflowV2ExecutionManager executionManager;
    private final TriggerModelHandler triggerModelHandler;

    private NodeExecutionContextService() {
        this.workflowV2Manager = WorkflowV2Manager.getInstance();
        this.executionManager = WorkflowV2ExecutionManager.getInstance();
        this.triggerModelHandler = TriggerModelHandler.getInstance();
    }

    private static final class Instance {
        private static final NodeExecutionContextService INSTANCE = new NodeExecutionContextService();
    }

    /**
     * Gets the instance of the {@link NodeExecutionContextService}.
     *
     * @return The instance of the {@link NodeExecutionContextService}.
     */
    public static NodeExecutionContextService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Puts the {@link NodeExecutionContext} into the service.
     *
     * @param executionId          Represents the execution id.
     * @param nodeExecutionContext Represents the {@link NodeExecutionContext}.
     */
    public void put(final long executionId, final NodeExecutionContext nodeExecutionContext) {
        executionManager.storeNodeContext(nodeExecutionContext.orgId(), executionId, toJson(nodeExecutionContext));
    }

    /**
     * Gets the {@link NodeExecutionContext} from the service.
     *
     * @param orgId       Represents the organization id.
     * @param executionId Represents the execution id.
     * @return The {@link NodeExecutionContext}.
     */
    public NodeExecutionContext get(final long orgId, final long executionId) {
        final String nodeContext = executionManager.getNodeContext(orgId, executionId);

        return toNodeContext(Json.object(nodeContext));
    }

    /**
     * Converts the {@link NodeExecutionContext} to JSON.
     *
     * @param context Represents the {@link NodeExecutionContext}.
     * @return The JSON representation of the {@link NodeExecutionContext}.
     */
    public JsonObject toJson(final NodeExecutionContext context) {
        final JsonObject json = Json.object()
                .put(WorkflowAttributesV2.ORG_ID, context.orgId())
                .put(WorkflowAttributesV2.USER_ID, context.userId())
                .put(WorkflowAttributesV2.EXECUTION_ID, context.executionId())
                .put(WorkflowAttributesV2.NODE_ID, context.nodeId())
                .put(WorkflowAttributesV2.NEED_NESTED_TRIGGER, context.isNeedNestedTrigger())
                .put(WorkflowAttributesV2.VERSION_ID, context.workflow().getWorkflowVersion().id())
                .put(ActionDataParserFields.USERS, convertUsersToJson(context.groupedUsers()));

        if (Objects.nonNull(context.nestedTriggerInfo())) {
            json.put(WorkflowAttributesV2.NESTED_TRIGGER_INFO, context.nestedTriggerInfo());
        }

        final Pair<TriggerSource, TlcModel> triggerSourceModelPair = context.triggerSourceModel();
        final Map<Long, Pair<String, TlcModel>> actionModels = context.getActionModels();

        if (Objects.nonNull(triggerSourceModelPair)) {
            json.put(WorkflowAttributesV2.TRIGGER_SOURCE_MODEL, Json.object()
                    .put(WorkflowAttributesV2.SOURCE, triggerSourceModelPair.getKey().getName())
                    .put(WorkflowAttributesV2.ENTITY_ID, triggerSourceModelPair.getValue().id()));
        }

        if (!actionModels.isEmpty()) {
            final JsonArray actionModelArray = Json.array();

            actionModels.forEach((nodeId, actionModel) ->
                    actionModelArray.put(Json.object()
                            .put(WorkflowAttributesV2.NODE_ID, nodeId)
                            .put(WorkflowAttributesV2.SOURCE, actionModel.getKey())
                            .put(WorkflowAttributesV2.ENTITY_ID, actionModel.getValue().id())));
            json.put(WorkflowAttributesV2.ACTION_MODELS, actionModels);
        }

        return json;
    }

    /**
     * Converts the JSON to {@link NodeExecutionContext}.
     *
     * @param json Represents the JSON.
     * @return The {@link NodeExecutionContext}.
     */
    public NodeExecutionContext toNodeContext(final JsonObject json) {
        final long orgId = json.getLong(WorkflowAttributesV2.ORG_ID);
        final long executionId = json.getLong(WorkflowAttributesV2.EXECUTION_ID);
        final long versionId = json.getLong(WorkflowAttributesV2.VERSION_ID);
        final WorkflowV2 workflow = workflowV2Manager.getByVersionId(orgId, versionId);
        final JsonObject triggerSourceModel = json.getJsonObject(WorkflowAttributesV2.TRIGGER_SOURCE_MODEL);
        final TriggerSource triggerSource = TriggerSource.getByName(triggerSourceModel.getString(WorkflowAttributesV2.SOURCE));
        final WorkflowNode triggerNode = workflow.getNodesAsCollection().stream().filter(workflowNode -> Objects.equals(NodeType.TRIGGER, workflowNode.getNodeType())).findFirst().orElse(null);
        final ExecutionEntity executionEntity = executionManager.getExecutionEntity(orgId, executionId, triggerNode.id());
        final ExecutionEntity.ExecutionSource executionSource = executionEntity.executionSource();
        final Pair<TlcModel, TlcModel> modelPair = switch (executionSource) {
            case COMPANY, CONTACT, DEAL, ACTIVITY ->
                    triggerModelHandler.fetchModelPair(orgId, executionEntity.entityId(), Source.get(executionSource.name()), executionEntity.entityJson());
            case EMAIL -> Pair.of(null, null);
        };
        final JsonArray actionModels = json.optJsonArray(WorkflowAttributesV2.ACTION_MODELS);
        final Map<Long, User> groupedUsers = convertToUsers(json.getJsonArray(ActionDataParserFields.USERS));
        final NodeExecutionContext nodeExecutionContext = new NodeExecutionContext(orgId, json.getLong(WorkflowAttributesV2.USER_ID), modelPair,
                json.getBoolean(WorkflowAttributesV2.NEED_NESTED_TRIGGER), json.optJsonObject(WorkflowAttributesV2.NESTED_TRIGGER_INFO), workflow, executionId,
                groupedUsers, Pair.of(triggerSource, modelPair.getKey()));

        nodeExecutionContext.setNodeId(json.getLong(WorkflowAttributesV2.NODE_ID));

        if (Objects.nonNull(actionModels)) {
            for (int index = 0; index < actionModels.size(); index++) {
                final JsonObject jsonObject = actionModels.getJsonObject(index);
                final String sourceName = jsonObject.getString(WorkflowAttributesV2.SOURCE);

                nodeExecutionContext.addActionModel(jsonObject.getLong(WorkflowAttributesV2.NODE_ID),
                        Pair.of(sourceName, triggerModelHandler
                                .fetchModel(orgId, jsonObject.getLong(WorkflowAttributesV2.ENTITY_ID), Source.get(sourceName))));
            }
        }
        return nodeExecutionContext;
    }

    /**
     * Converts the users to JSON.
     *
     * @param users Represents the users.
     * @return The JSON representation of the users.
     */
    private JsonArray convertUsersToJson(final Map<Long, User> users) {
        final JsonArray jsonArray = Json.array();

        for (final User user : users.values()) {
            final JsonObject userJson = Json.object();

            userJson.put(ActionDataParserFields.ID, user.id());
            userJson.put(ActionDataParserFields.FIRST_NAME, user.getFirstName());
            userJson.put(ActionDataParserFields.LAST_NAME, user.getLastName());
            userJson.put(ActionDataParserFields.EMAIL, user.getEmail());
            userJson.put(ActionDataParserFields.PHONE, user.getMobile());
            userJson.put(ActionDataParserFields.STATE, user.getUserState().id());

            jsonArray.put(userJson);
        }

        return jsonArray;
    }

    /**
     * Converts the json array to user.
     *
     * @param jsonArray Represents the json array which holds the user details.
     * @return The users grouped by their IDs, with each ID serving as a key and its corresponding user as the value.
     */
    private Map<Long, User> convertToUsers(final JsonArray jsonArray) {
        final Map<Long, User> groupedUsers = new HashMap<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject userJson = jsonArray.getJsonObject(index);
            final User user = new User();
            final Long id = userJson.getLong(ActionDataParserFields.ID);

            user.setId(id);
            user.setFirstName(userJson.optString(ActionDataParserFields.FIRST_NAME, ""));
            user.setLastName(userJson.optString(ActionDataParserFields.LAST_NAME, ""));
            user.setEmail(userJson.optString(ActionDataParserFields.EMAIL, ""));
            user.setMobile(userJson.optString(ActionDataParserFields.PHONE, ""));
            user.setUserState(User.State.get(userJson.getShort(ActionDataParserFields.STATE)));

            groupedUsers.put(id, user);
        }

        return groupedUsers;
    }
}

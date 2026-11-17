package com.tlc.crm.workflow.internal.trigger.action;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.iam.common.models.User;
import com.tlc.validator.TlcModel;

import com.tlc.crm.workflow.api.model.TriggerSource;
import com.tlc.crm.workflow.internal.trigger.action.navigate.ActionNavigator;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionDataParserFields;
import com.tlc.crm.workflow.internal.trigger.action.tracker.ActionFailureCauseTracker;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the {@link ActionExecutionContext} encompassing essential
 * details required for executing workflow actions.
 *
 * @param triggerModel              Refers the model {@link Pair} of an entity from which the workflow is triggered.
 * @param actionNavigator           Refers the {@link ActionNavigator} which navigates through workflow action.
 * @param groupedUsers              Refers the grouped users details.
 * @param executionHistoryId        Refers the history id.
 * @param createdEntities           Refers the entities that are created via workflow action execution.
 * @param actionJsonContext         Refers the {@link ActionJsonContext} which holds the converted action repository data.
 * @param actionFailureCauseTracker Refers the {@link ActionFailureCauseTracker} for track the error if occurred.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ActionExecutionContext(Pair<TriggerSource, TlcModel> triggerModel,
                                     ActionNavigator actionNavigator,
                                     Map<Long, User> groupedUsers, Long executionHistoryId,
                                     List<Pair<String, TlcModel>> createdEntities,
                                     ActionJsonContext actionJsonContext,
                                     ActionFailureCauseTracker actionFailureCauseTracker, Map<Long, Long> entityStateIdMap) {

    public ActionExecutionContext(final Pair<TriggerSource, TlcModel> triggerModel, final ActionNavigator actionNavigator,
                                  final Map<Long, User> groupedUsers, final Long executionHistoryId,
                                  final List<Pair<String, TlcModel>> createdEntities, final JsonObject jsonObject, final Map<Long, Long> entityStateIdMap) {
        this(triggerModel, actionNavigator, groupedUsers, executionHistoryId, createdEntities, new ActionJsonContext(jsonObject), ActionFailureCauseTracker.create(), entityStateIdMap);
    }

    /**
     * {@inheritDoc}
     *
     * @return The history id.
     */
    public Long executionHistoryId() {
        return executionHistoryId;
    }

    /**
     * {@inheritDoc}
     *
     * @param createdEntity Refers the {@link Pair} of action source and its relevant entity.
     */
    public void addCreatedEntity(final Pair<String, TlcModel> createdEntity) {
        createdEntities.add(createdEntity);
    }

    /**
     * {@inheritDoc}
     *
     * @return The trigger model.
     */
    public Pair<TriggerSource, TlcModel> triggerModel() {
        return triggerModel;
    }

    /**
     * {@inheritDoc}
     *
     * @return The grouped users.
     */
    public Map<Long, User> groupedUsers() {
        return groupedUsers;
    }

    /**
     * {@inheritDoc}
     *
     * @param index Refers the index.
     * @return The created entity.
     */
    public Pair<String, TlcModel> retrieveCreatedEntity(final int index) {
        return createdEntities.get(index);
    }

    /**
     * {@inheritDoc}
     *
     * @return The list of created entity.
     */
    public List<Pair<String, TlcModel>> retrieveCreatedEntities() {
        return createdEntities;
    }

    /**
     * {@inheritDoc}
     *
     * @return The reason tracker.
     */
    public ActionFailureCauseTracker tracker() {
        return actionFailureCauseTracker;
    }

    /**
     * {@inheritDoc}
     *
     * @return The action navigator.
     */
    public ActionNavigator navigator() {
        return actionNavigator;
    }

    /**
     * Holds the converted {@link JsonObject} of action context.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    final static class ActionJsonContext {

        private final JsonObject jsonObject;
        private final ActionDataParser actionDataParser;

        private ActionJsonContext(final JsonObject jsonObject) {
            this.jsonObject = jsonObject;
            this.actionDataParser = ActionDataParser.getInstance();
        }

        /**
         * Adds the model by converts into the {@link JsonObject}.
         *
         * @param createdModelPair Refers the created model pair.
         */
        void addCreatedModelJson(final Pair<String, TlcModel> createdModelPair) {
            final JsonArray createdEntities = jsonObject.optJsonArray(ActionDataParserFields.CREATED_ENTITIES);
            final JsonObject createdModelJson = actionDataParser.convertModelPairToJson(createdModelPair);

            if (Objects.nonNull(createdEntities)) {
                createdEntities.put(createdModelJson);
            } else {
                jsonObject.put(ActionDataParserFields.CREATED_ENTITIES, Json.array().put(createdModelJson));
            }
        }

        /**
         * Removes the executed workflow action id presented in the {@link JsonObject}.
         *
         * @param workflowActionId Refers the workflowId to be removed.
         */
        void removeWorkflowActionId(final Long workflowActionId) {
            jsonObject.getJsonArray(ActionDataParserFields.WORKFLOW_ACTIONS).remove(workflowActionId);
        }

        /**
         * Gets the json object.
         *
         * @return The json object.
         */
        JsonObject get() {
            return jsonObject;
        }
    }
}

package com.tlc.crm.workflow.internal.trigger.action;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.validator.TlcModel;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.workflow.internal.trigger.action.converter.ActionDataParserImpl;

/**
 * The parser which facilitates the conversion of data form
 * {@link ActionExecutionContext} to {@link JsonObject} and vice versa.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ActionDataParser {

    /**
     * Gets the instance of {@link ActionDataParser}.
     *
     * @return The {@link ActionDataParser} instance.
     */
    static ActionDataParser getInstance() {
        return ActionDataParserImpl.getInstance();
    }

    /**
     * Converts the {@link ActionExecutionContext} into {@link JsonObject}.
     *
     * @param orgId                  Refers the organization id.
     * @param workflow               Refers the workflow.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @return The converted json.
     */
    JsonObject wrapToJson(final Long orgId, final Workflow workflow, final ActionExecutionContext actionExecutionContext);

    /**
     * Converts the {@link JsonObject} into {@link ActionExecutionContext}.
     *
     * @param jsonObject Refers the {@link JsonObject} which holds the action requisite data.
     * @return The {@link ActionExecutionContext}.
     */
    ActionExecutionContext unwrapJson(final JsonObject jsonObject);

    /**
     * Converts the model which is created from the workflow execution to {@link JsonObject}.
     *
     * @param createdModelPair Refers the model which is created from the workflow execution.
     * @return The converted json.
     */
    JsonObject convertModelPairToJson(final Pair<String, TlcModel> createdModelPair);
}

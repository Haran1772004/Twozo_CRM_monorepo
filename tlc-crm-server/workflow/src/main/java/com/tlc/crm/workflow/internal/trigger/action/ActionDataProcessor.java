package com.tlc.crm.workflow.internal.trigger.action;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.internal.trigger.action.converter.ActionDataProcessorImpl;

/**
 * Facilitates the conversion process of data required for the workflow action triggering.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ActionDataProcessor {

    /**
     * Gets the instance of {@link ActionDataProcessor}.
     *
     * @return The {@link ActionDataProcessor} instance.
     */
    static ActionDataProcessor getInstance() {
        return ActionDataProcessorImpl.getInstance();
    }

    /**
     * Encapsulates the provided data into {@link JsonObject}.
     *
     * @param orgId                  Refers the organization id.
     * @param workflow               Refers the workflow.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext} which is to be converted.
     */
    JsonObject wrapData(final Long orgId, final Workflow workflow, final ActionExecutionContext actionExecutionContext);

    /**
     * Gets the {@link ActionExecutionContext} by converting the given {@link JsonObject}.
     *
     * @param jsonObject Refers the json object to be unwrapped.
     * @return The converted {@link ActionExecutionContext}.
     */
    ActionExecutionContext unwrapData(final JsonObject jsonObject);
}

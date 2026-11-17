package com.tlc.crm.workflow.internal.trigger.action.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.Workflow;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.ActionDataProcessor;
import com.tlc.crm.workflow.internal.trigger.action.ActionDataParser;

/**
 * Facilitates the conversion process of data required for the workflow action triggering.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionDataProcessorImpl implements ActionDataProcessor {

    private final ActionDataParser actionDataParser;

    private ActionDataProcessorImpl() {
        this.actionDataParser = ActionDataParser.getInstance();
    }

    private static class Instance {
        private static final ActionDataProcessorImpl INSTANCE = new ActionDataProcessorImpl();
    }

    public static ActionDataProcessorImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param workflow               Refers the workflow.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext} which is to be converted.
     */
    @Override
    public JsonObject wrapData(final Long orgId, final Workflow workflow, final ActionExecutionContext actionExecutionContext) {
        return actionDataParser.wrapToJson(orgId, workflow, actionExecutionContext);
    }

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Refers the json object to be unwrapped.
     * @return The converted {@link ActionExecutionContext}.
     */
    @Override
    public ActionExecutionContext unwrapData(final JsonObject jsonObject) {
        return actionDataParser.unwrapJson(jsonObject);
    }
}

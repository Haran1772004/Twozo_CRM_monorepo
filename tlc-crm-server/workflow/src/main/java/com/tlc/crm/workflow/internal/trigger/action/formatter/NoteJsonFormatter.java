package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.function.Function;

/**
 * Converting the JSON representation of action field properties to match
 * the JSON structure of note entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class NoteJsonFormatter extends AbstractJsonFormatter {

    private NoteJsonFormatter() {
    }

    private static class Instance {
        private static final NoteJsonFormatter NOTE_JSON_FORMATTER = new NoteJsonFormatter();
    }

    /**
     * Gets the instance of {@link NoteJsonFormatter}.
     *
     * @return {@link NoteJsonFormatter} instance.
     */
    public static NoteJsonFormatter getInstance() {
        return Instance.NOTE_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                  Refers the organization id.
     * @param workflowActionId       Refers the workflow action id.
     * @param fieldMappingJson       Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param actionExecutionContext Refers the {@link ActionExecutionContext}.
     * @param actionSource           Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final ActionExecutionContext actionExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject noteJson = Json.object();
        final Function<JsonArray, Object> valueRetriever = (jsonArray) -> {
            final JsonObject jsonObject = jsonArray.getJsonObject(0);

            if (jsonObject.containsKey(ActionAttributes.DEPENDS_ON)) {
                return getStepOptionValue(jsonObject, actionSource, actionExecutionContext);
            } else {
                return jsonObject.get(ActionAttributes.VALUE);
            }
        };

        for (int index = 0; index < fieldMappingJson.size(); index++) {
            final JsonArray fieldArray = fieldMappingJson.getJsonArray(index);

            for (int fieldIndex = 0; fieldIndex < fieldArray.size(); fieldIndex++) {
                final JsonObject fieldObject = fieldArray.getJsonObject(fieldIndex);
                final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);
                final WorkflowField workflowField = workflowFieldService.get(fieldObject.getLong(ActionAttributes.FIELD_ID));

                switch (workflowField.getAliasName()) {
                    case "note" -> noteJson.put(ActionAttributes.CONTENT, valueRetriever.apply(valueArray));
                    case "contact", "company", "deal" ->
                            noteJson.put(ActionAttributes.ENTITY_ID, valueRetriever.apply(valueArray));
                }
            }
        }

        return noteJson;
    }
}

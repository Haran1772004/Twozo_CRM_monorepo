package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import java.util.function.Function;

/**
 * Formats the note action field mapping JSON for V2 execution.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class NoteJsonFormatterV2 extends AbstractJsonFormatterV2 {

    private NoteJsonFormatterV2() {
    }

    private static class Instance {
        private static final NoteJsonFormatterV2 NOTE_JSON_FORMATTER = new NoteJsonFormatterV2();
    }

    /**
     * Gets the instance of {@link NoteJsonFormatterV2}.
     *
     * @return {@link NoteJsonFormatterV2} instance.
     */
    public static NoteJsonFormatterV2 getInstance() {
        return Instance.NOTE_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Refers the organization id.
     * @param workflowActionId     Refers the workflow action id.
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link ActionExecutionContext}.
     * @param actionSource         Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject noteJson = Json.object();
        final Function<JsonArray, Object> valueRetriever = (jsonArray) -> {
            final JsonObject jsonObject = jsonArray.getJsonObject(0);

            if (jsonObject.containsKey(ActionAttributes.DEPENDS_ON)) {
                return getStepOptionValue(jsonObject, actionSource, nodeExecutionContext);
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
                    case WorkflowAttributesV2.NOTE ->
                            noteJson.put(ActionAttributes.CONTENT, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.COMPANY, WorkflowAttributesV2.DEAL, WorkflowAttributesV2.CONTACT ->
                            noteJson.put(ActionAttributes.ENTITY_ID, valueRetriever.apply(valueArray));
                }
            }
        }

        return noteJson;
    }
}

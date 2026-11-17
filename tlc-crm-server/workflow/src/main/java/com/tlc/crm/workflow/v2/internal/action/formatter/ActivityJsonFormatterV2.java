package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.features.api.activity.model.Status;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.v2.field.WorkflowAttributesV2;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Formats the activity action field mapping JSON for V2 execution.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class ActivityJsonFormatterV2 extends AbstractJsonFormatterV2 {

    private ActivityJsonFormatterV2() {
    }

    private static class Instance {
        private static final ActivityJsonFormatterV2 ACTIVITY_JSON_FORMATTER = new ActivityJsonFormatterV2();
    }

    /**
     * Gets the instance of {@link ActivityJsonFormatterV2}.
     *
     * @return {@link ActivityJsonFormatterV2} instance.
     */
    public static ActivityJsonFormatterV2 getInstance() {
        return Instance.ACTIVITY_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Refers the organization id.
     * @param workflowActionId     Refers the workflow action id.
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext}.
     * @param actionSource         Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson, final NodeExecutionContext nodeExecutionContext,
                             final TriggerActionSource actionSource) {
        final JsonObject activityJson = Json.object();
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
                final WorkflowField workflowField = workflowFieldService.get(fieldObject.getLong(ActionAttributes.FIELD_ID));
                final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);

                switch (workflowField.getAliasName()) {
                    case WorkflowAttributesV2.ID ->
                            activityJson.put(ActionAttributes.ID, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.TITLE ->
                            activityJson.put(ActionAttributes.TITLE, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.ACTIVITY_TYPE ->
                            activityJson.put(ActionAttributes.TYPE_ID, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.START_TIME ->
                            activityJson.put(ActionAttributes.START_TIME, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.END_TIME ->
                            activityJson.put(ActionAttributes.END_TIME, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.ASSIGNEE ->
                            activityJson.put(ActionAttributes.ASSIGNEE_ID, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.AVAILABILITY ->
                            activityJson.put(ActionAttributes.AVAILABILITY, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.REMINDER ->
                            activityJson.put(ActionAttributes.REMINDER, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.PRIORITY ->
                            activityJson.put(ActionAttributes.PRIORITY, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.COLLABORATORS -> {
                        final Collection<Long> collaborators = valueArray.toList().stream()
                                .map(json -> NumberUtils.parseNumber(((Map<String, ?>) json).get(ActionAttributes.VALUE)).longValue()).toList();

                        activityJson.put(ActionAttributes.COLLABORATORS, collaborators);
                    }
                    case WorkflowAttributesV2.LOCATION ->
                            activityJson.put(ActionAttributes.LOCATION, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.DESCRIPTION ->
                            activityJson.put(ActionAttributes.DESCRIPTION, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.NOTE ->
                            activityJson.put(ActionAttributes.NOTE, Json.object().put(ActionAttributes.CONTENT, valueRetriever.apply(valueArray)));
                    case WorkflowAttributesV2.STATUS -> {
                        final JsonObject valueJson = valueArray.getJsonObject(0);

                        if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                            final int statusId = (Integer) getStepOptionValue(valueJson, actionSource, nodeExecutionContext);

                            activityJson.put(ActionAttributes.IS_DONE, Status.COMPLETED == Status.get(statusId));
                        } else {
                            activityJson.put(ActionAttributes.IS_DONE, Status.COMPLETED == Status.get((int) valueJson.get(ActionAttributes.VALUE)));
                        }
                    }
                    case WorkflowAttributesV2.CONTACT -> {
                        final Collection<Long> contactIds = new ArrayList<>();

                        for (int i = 0; i < valueArray.size(); i++) {
                            final JsonObject valueJson = valueArray.getJsonObject(i);

                            if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                                final Object activityContacts = getStepOptionValue(valueJson, actionSource, nodeExecutionContext);

                                if (activityContacts instanceof Collection<?> values) {
                                    contactIds.addAll((Collection<Long>) values);
                                } else if (activityContacts instanceof Number value) {
                                    contactIds.add(value.longValue());
                                }
                            } else {
                                contactIds.add(NumberUtils.parseNumber(valueJson.get(ActionAttributes.VALUE)).longValue());
                            }
                        }
                        activityJson.put(ActionAttributes.CONTACT_IDS, contactIds);
                    }
                    case WorkflowAttributesV2.COMPANY ->
                            activityJson.put(ActionAttributes.COMPANY_ID, valueRetriever.apply(valueArray));
                    case WorkflowAttributesV2.DEAL ->
                            activityJson.put(ActionAttributes.DEAL_ID, valueRetriever.apply(valueArray));
                }
            }
        }

        return activityJson;
    }
}

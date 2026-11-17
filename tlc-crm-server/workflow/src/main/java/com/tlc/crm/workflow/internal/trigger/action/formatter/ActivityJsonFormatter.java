package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Converting the JSON representation of action field properties to match
 * the JSON structure of activity entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActivityJsonFormatter extends AbstractJsonFormatter {

    private ActivityJsonFormatter() {
    }

    private static class Instance {
        private static final ActivityJsonFormatter ACTIVITY_JSON_FORMATTER = new ActivityJsonFormatter();
    }

    /**
     * Gets the instance of {@link ActivityJsonFormatter}.
     *
     * @return {@link ActivityJsonFormatter} instance.
     */
    public static ActivityJsonFormatter getInstance() {
        return Instance.ACTIVITY_JSON_FORMATTER;
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
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson, final ActionExecutionContext actionExecutionContext,
                             final TriggerActionSource actionSource) {
        final JsonObject activityJson = Json.object();
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
                final WorkflowField workflowField = workflowFieldService.get(fieldObject.getLong(ActionAttributes.FIELD_ID));
                final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);

                switch (workflowField.getAliasName()) {
                    case "id" -> activityJson.put(ActionAttributes.ID, valueRetriever.apply(valueArray));
                    case "title" -> activityJson.put(ActionAttributes.TITLE, valueRetriever.apply(valueArray));
                    case "activityType" -> activityJson.put(ActionAttributes.TYPE_ID, valueRetriever.apply(valueArray));
                    case "startTime" -> activityJson.put(ActionAttributes.START_TIME, valueRetriever.apply(valueArray));
                    case "endTime" -> activityJson.put(ActionAttributes.END_TIME, valueRetriever.apply(valueArray));
                    case "assignee" -> activityJson.put(ActionAttributes.ASSIGNEE_ID, valueRetriever.apply(valueArray));
                    case "availability" ->
                            activityJson.put(ActionAttributes.AVAILABILITY, valueRetriever.apply(valueArray));
                    case "reminder" -> activityJson.put(ActionAttributes.REMINDER, valueRetriever.apply(valueArray));
                    case "priority" -> activityJson.put(ActionAttributes.PRIORITY, valueRetriever.apply(valueArray));
                    case "collaborators" -> {
                        final Collection<Long> collaborators = valueArray.toList().stream()
                                .map(json -> NumberUtils.parseNumber(((Map<String, ?>) json).get(ActionAttributes.VALUE)).longValue()).toList();

                        activityJson.put(ActionAttributes.COLLABORATORS, collaborators);
                    }
                    case "location" -> activityJson.put(ActionAttributes.LOCATION, valueRetriever.apply(valueArray));
                    case "description" ->
                            activityJson.put(ActionAttributes.DESCRIPTION, valueRetriever.apply(valueArray));
                    case "note" -> activityJson.put(ActionAttributes.NOTE, Json.object().put(ActionAttributes.CONTENT, valueRetriever.apply(valueArray)));
                    case "status" -> {
                        final JsonObject valueJson = valueArray.getJsonObject(0);

                        if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                            final int statusId = (Integer) getStepOptionValue(valueJson, actionSource, actionExecutionContext);

                            activityJson.put(ActionAttributes.IS_DONE, Status.COMPLETED == Status.get(statusId));
                        } else {
                            activityJson.put(ActionAttributes.IS_DONE, Status.COMPLETED == Status.get((int) valueJson.get(ActionAttributes.VALUE)));
                        }
                    }
                    case "contact" -> {
                        final Collection<Long> contactIds = new ArrayList<>();

                        for (int i = 0; i < valueArray.size(); i++) {
                            final JsonObject valueJson = valueArray.getJsonObject(i);

                            if (valueJson.containsKey(ActionAttributes.DEPENDS_ON)) {
                                final Object activityContacts = getStepOptionValue(valueJson, actionSource, actionExecutionContext);

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
                    case "company" -> activityJson.put(ActionAttributes.COMPANY_ID, valueRetriever.apply(valueArray));
                    case "deal" -> activityJson.put(ActionAttributes.DEAL_ID, valueRetriever.apply(valueArray));
                }
            }
        }

        return activityJson;
    }
}

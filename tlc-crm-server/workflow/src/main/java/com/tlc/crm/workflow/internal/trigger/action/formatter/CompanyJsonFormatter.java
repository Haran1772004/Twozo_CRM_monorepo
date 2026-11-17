package com.tlc.crm.workflow.internal.trigger.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.internal.trigger.action.ActionExecutionContext;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Converting the JSON representation of action field properties to match
 * the JSON structure of company entity.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CompanyJsonFormatter extends AbstractJsonFormatter {

    private final EntityJsonAggregator companyJsonAggregator;

    private CompanyJsonFormatter() {
        this.companyJsonAggregator = CompanyJsonAggregator.getInstance();
    }

    private static class Instance {
        private static final CompanyJsonFormatter COMPANY_JSON_FORMATTER = new CompanyJsonFormatter();
    }

    /**
     * Gets the instance of {@link CompanyJsonFormatter}.
     *
     * @return {@link CompanyJsonFormatter} instance.
     */
    public static CompanyJsonFormatter getInstance() {
        return Instance.COMPANY_JSON_FORMATTER;
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
        final JsonObject companyJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, ActionAttributes.COMPANY);
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
                final Optional<FieldInfo> optionalField = fields.stream().filter(field ->
                        Objects.equals(fieldObject.getLong(ActionAttributes.FIELD_ID), field.id())).findFirst();

                if (optionalField.isPresent()) {
                    final FieldInfo fieldInfo = optionalField.get();
                    final JsonArray valueArray = fieldObject.getJsonArray(ActionAttributes.VALUES);

                    if (fieldInfo.isPersistent()) {
                        loadPersistentFieldValue(orgId, fieldInfo, valueArray, companyJson);
                    } else {
                        switch (fieldInfo.aliasName()) {
                            case "id" -> companyJson.put(ActionAttributes.ID, valueRetriever.apply(valueArray));
                            case "name", "website", "salesOwner" ->
                                    companyJson.put(fieldInfo.idAsString(), valueRetriever.apply(valueArray));
                            case "address" -> {
                                final JsonObject subFieldJson = Json.object();
                                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                                subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(),
                                        valueArray.getJsonObject(0).get(ActionAttributes.VALUE));
                                companyJson.put(fieldInfo.idAsString(), subFieldJson);
                            }
                            case "tags" -> {
                                final Collection<Object> tagIds = valueArray.toList().stream().map(json -> ((Map<String, ?>) json).get(ActionAttributes.VALUE)).collect(Collectors.toSet());
                                final JsonArray tagArray = Json.array();

                                for (final Object tagId : tagIds) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(ActionAttributes.VALUE, tagId);
                                    tagArray.put(tagJson);
                                }

                                companyJson.put(fieldInfo.idAsString(), tagArray);
                            }
                        }
                    }
                }
            }
        }
        companyJsonAggregator.merge(orgId, companyJson);

        return companyJson;
    }
}

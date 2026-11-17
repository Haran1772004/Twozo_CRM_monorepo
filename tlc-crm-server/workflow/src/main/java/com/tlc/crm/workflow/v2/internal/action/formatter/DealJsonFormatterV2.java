package com.tlc.crm.workflow.v2.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.field.deal.DealFields;

import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.workflow.api.model.TriggerActionSource;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.internal.trigger.action.field.ActionAttributes;
import com.tlc.crm.workflow.internal.trigger.action.formatter.DealJsonAggregator;
import com.tlc.crm.workflow.internal.trigger.action.formatter.EntityJsonAggregator;
import com.tlc.crm.workflow.v2.internal.execution.NodeExecutionContext;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Formats the deal action field mapping JSON for V2 execution.
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class DealJsonFormatterV2 extends AbstractJsonFormatterV2 {

    private final EntityJsonAggregator dealJsonAggregator;

    private DealJsonFormatterV2() {
        this.dealJsonAggregator = DealJsonAggregator.getInstance();
    }

    private static class Instance {
        private static final DealJsonFormatterV2 DEAL_JSON_FORMATTER = new DealJsonFormatterV2();
    }

    /**
     * Gets the instance of {@link DealJsonFormatterV2}.
     *
     * @return {@link DealJsonFormatterV2} instance.
     */
    public static DealJsonFormatterV2 getInstance() {
        return Instance.DEAL_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                Refers the organization id.
     * @param workflowActionId     Refers the workflow action id.
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext}.
     * @return The formatted {@link JsonObject}.
     */
    @Override
    public JsonObject format(final Long orgId, final Long workflowActionId, final JsonArray fieldMappingJson,
                             final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject dealJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, ActionAttributes.DEAL);
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
                final Optional<FieldInfo> optionalField = fields.stream().filter(field ->
                        Objects.equals(fieldObject.getLong(ActionAttributes.FIELD_ID), field.id())).findFirst();

                if (optionalField.isPresent()) {
                    final FieldInfo fieldInfo = optionalField.get();
                    final JsonArray valueArray = fieldObject.optJsonArray(ActionAttributes.VALUES);

                    if (fieldInfo.isPersistent()) {
                        loadPersistentFieldValue(orgId, fieldInfo, valueArray, dealJson);
                    } else {
                        switch (fieldInfo.aliasName()) {
                            case DealFields.ID -> dealJson.put(ActionAttributes.ID, valueRetriever.apply(valueArray));
                            case DealFields.TITLE, DealFields.PIPELINE_STAGE, DealFields.WON_REASON,
                                 DealFields.LOST_REASON, DealFields.SALES_OWNER, DealFields.PRIMARY_CONTACT,
                                 DealFields.PIPELINE, DealFields.COMPANY, DealFields.DEAL_CLOSED_ON ->
                                    dealJson.put(fieldInfo.idAsString(), valueRetriever.apply(valueArray));
                            case DealFields.TAGS -> {
                                final Collection<Object> tagIds = valueArray.toList().stream()
                                        .map(json -> ((Map<String, ?>) json).get(ActionAttributes.VALUE)).collect(Collectors.toSet());
                                final JsonArray tagArray = Json.array();

                                for (final Object tagId : tagIds) {
                                    final JsonObject tagJson = Json.object();

                                    tagJson.put(ActionAttributes.VALUE, tagId);
                                    tagArray.put(tagJson);
                                }
                                dealJson.put(fieldInfo.idAsString(), tagArray);
                            }
                            case DealFields.RELATED_CONTACTS -> {
                                final JsonArray participants = Json.array();

                                for (int j = 0; j < valueArray.size(); j++) {
                                    final JsonObject valueJson = valueArray.getJsonObject(j);
                                    final JsonObject jsonObject = Json.object();

                                    jsonObject.put(ActionAttributes.ID, valueJson.getLong(ActionAttributes.VALUE));
                                    participants.put(jsonObject);
                                }
                                dealJson.put(fieldInfo.idAsString(), participants);
                            }
                        }
                    }
                }
            }
        }
        dealJsonAggregator.merge(orgId, dealJson);

        return dealJson;
    }

    /**
     * Converts the fields mapping json into product detail {@link JsonObject}.
     *
     * @param orgId                Refers the organization id.
     * @param fieldMappingJson     Refers the {@link JsonArray} which holds information about the data pertaining to actions.
     * @param nodeExecutionContext Refers the {@link NodeExecutionContext}.
     * @param actionSource         Refers the {@code TriggerActionSource} for which the step options are retrieved.
     * @return The formatted {@link JsonObject}.
     */
    public JsonObject formatAddProductJson(final Long orgId, final JsonArray fieldMappingJson,
                                           final NodeExecutionContext nodeExecutionContext, final TriggerActionSource actionSource) {
        final JsonObject productDetailJson = Json.object();
        final JsonArray productItems = Json.array();
        final JsonObject productJson = Json.object();
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
                    case "deal" -> productDetailJson.put(ActionAttributes.DEAL_ID, valueRetriever.apply(valueArray));
                    case "product" -> {
                        productJson.put(ActionAttributes.PRODUCT, ((Number) valueRetriever.apply(valueArray)).longValue());
                    }
                    case "unitPrice" -> {
                        productJson.put(ActionAttributes.PRICE, valueRetriever.apply(valueArray));
                    }
                    case "quantity" -> {
                        productJson.put(ActionAttributes.QUANTITY, valueRetriever.apply(valueArray));
                    }
                }
            }
        }
        productItems.put(productJson);
        productDetailJson.put(ActionAttributes.CURRENCY, baseCurrencyService.getDefaultCurrency(orgId).getId());
        productDetailJson.put(ActionAttributes.ITEMS, productItems);

        return productDetailJson;
    }
}

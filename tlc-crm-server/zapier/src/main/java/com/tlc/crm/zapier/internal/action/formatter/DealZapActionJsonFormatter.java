package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.api.models.ZapierField;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of {@link DealZapActionJsonFormatter} for formatting
 * deal-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DealZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final DealZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new DealZapActionJsonFormatter();
    }

    public static DealZapActionJsonFormatter getInstance() {
        return Instance.ZAP_ACTION_JSON_FORMATTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      The ID of the organization, used to apply organization-specific logic or formatting.
     * @param zapAction  The Zapier action containing data to be formatted.
     * @param actionJson The existing JSON object that will be populated with the formatted action data.
     * @return The formatted JSON object ready for sending as a Zapier action payload.
     */
    @Override
    public JsonObject format(final long orgId, final ZapAction zapAction, final JsonObject actionJson) {
        final JsonObject dealJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.DEAL.getName());

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, dealJson, actionJson);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "title", "description" -> {
                        if (actionJson.containsKey(aliasName)) {
                            dealJson.put(fieldInfo.idAsString(), actionJson.getString(aliasName));
                        }
                    }
                    case "pipeline", "pipelineStage", "primaryContact", "company",
                         "wonReason", "lostReason", "salesOwner", "dealSource", "territory",
                         "type", "paymentStatus" -> {
                        if (actionJson.containsKey(aliasName)) {
                            dealJson.put(fieldInfo.idAsString(), actionJson.getLong(aliasName));
                        }
                    }
                    case "id" -> {
                        if (actionJson.containsKey(aliasName)) {
                            dealJson.put(ZapAttributes.ID, actionJson.getLong(aliasName));
                        }
                    }
                    case "dealClosedOn", "expectedCloseDate" -> {
                        if (actionJson.containsKey(aliasName)) {
                            dealJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(aliasName)));
                        }
                    }
                    case "relatedContacts" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray relatedContacts = Json.array();

                            actionJson.getJsonArray(aliasName).forEach(value -> relatedContacts.put(NumberUtils.parseNumber(value).longValue()));
                            dealJson.put(fieldInfo.idAsString(), relatedContacts);
                        }
                    }
                    case "dealValue" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), actionJson.getLong(aliasName));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), baseCurrencyService.getDefaultCurrency(orgId).getId());

                            dealJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case "tags" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final JsonArray tags = Json.array();

                            actionJson.getJsonArray(aliasName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(ZapAttributes.ID, NumberUtils.parseNumber(value).longValue());
                                tags.put(jsonObject);
                            });

                            dealJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                }
            }
        }
        return dealJson;
    }

    /**
     * Formats the input JSON for the "Add Product" Zapier action.
     *
     * @param orgId      The ID of the organization for which the product is being added.
     * @param zapAction  The Zapier action metadata and configuration.
     * @param actionJson The input JSON representing product details from Zapier.
     * @return The {@link JsonObject} representing the formatted product data.
     */
    public JsonObject formatAddProductJson(final long orgId, final ZapAction zapAction, final JsonObject actionJson) {
        final JsonObject productDetailJson = Json.object();
        final JsonArray productItems = Json.array();
        final JsonObject productJson = Json.object();

        for (final ZapierField zapierField : zapierFieldService.getFieldsByActionId(zapAction.getId())) {
            final String aliasName = zapierField.getAliasName();

            switch (aliasName) {
                case "deal" -> productDetailJson.put(ZapAttributes.DEAL_ID, actionJson.getLong(aliasName));
                case "product" -> productJson.put(ZapAttributes.PRODUCT, actionJson.getLong(aliasName));
                case "unitPrice" -> productJson.put(ZapAttributes.PRICE, actionJson.getLong(aliasName));
                case "quantity" -> productJson.put(ZapAttributes.QUANTITY, actionJson.getLong(aliasName));
            }
        }
        productItems.put(productJson);
        productDetailJson.put(ZapAttributes.CURRENCY, baseCurrencyService.getDefaultCurrency(orgId).getId());
        productDetailJson.put(ZapAttributes.ITEMS, productItems);

        return productDetailJson;
    }
}

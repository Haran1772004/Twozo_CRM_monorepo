package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ProductZapActionJsonFormatter} for formatting
 * product-related data into a JSON structure suitable for Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductZapActionJsonFormatter extends AbstractZapActionJsonFormatter {

    private static final class Instance {
        private static final ProductZapActionJsonFormatter ZAP_ACTION_JSON_FORMATTER = new ProductZapActionJsonFormatter();
    }

    public static ProductZapActionJsonFormatter getInstance() {
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
        final JsonObject productJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.PRODUCT.getName());

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, productJson, actionJson);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "name", "description", "skuNumber", "productCode" -> {
                        if (actionJson.containsKey(aliasName)) {
                            productJson.put(fieldInfo.idAsString(), actionJson.getString(aliasName));
                        }
                    }
                    case "salesOwner", "category", "activeStatus", "taxable",
                         "quantityOrdered", "quantityInStock", "type" -> {
                        if (actionJson.containsKey(aliasName)) {
                            productJson.put(fieldInfo.idAsString(), actionJson.getLong(aliasName));
                        }
                    }
                    case "id" -> {
                        if (actionJson.containsKey(aliasName)) {
                            productJson.put(ZapAttributes.ID, actionJson.getLong(aliasName));
                        }
                    }
                    case "priceTags" -> {
                        if (actionJson.containsKey(aliasName)) {
                            final Map<StoreSubType, FieldSubType> priceTagSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PRICE_TAGS);
                            final List<Currency> activeCurrencies = baseCurrencyService.getActiveCurrencies(orgId).stream().toList();
                            final JsonArray array = Json.array();
                            final String priceKey = priceTagSubtypes.get(StoreSubType.UNIT_PRICE).idAsString();
                            final String currencyKey = priceTagSubtypes.get(StoreSubType.CURRENCY).idAsString();
                            final Counter counter = new Counter(0);

                            actionJson.getJsonArray(aliasName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(priceKey, NumberUtils.parseNumber(value).longValue());
                                jsonObject.put(currencyKey, activeCurrencies.get(counter.getAndIncrement()).getId());
                                array.put(jsonObject);
                            });

                            productJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case "validTill" -> {
                        if (actionJson.containsKey(aliasName)) {
                            productJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(aliasName)));
                        }
                    }
                }
            }
        }
        return productJson;
    }
}

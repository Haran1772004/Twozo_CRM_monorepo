package com.tlc.crm.core.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.core.field.product.ProductFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Formatter that converts an incoming REST request JSON for a product into the
 * internal request JSON structure used by the entity layer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ProductRestReqFormatter extends EntityReqJsonFormatter {

    private ProductRestReqFormatter() {
    }

    private static class Instance {
        private static final ProductRestReqFormatter INSTANCE = new ProductRestReqFormatter();
    }

    public static ProductRestReqFormatter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization ID for which the formatting is being done, allowing for organization-specific logic if necessary.
     * @param requestJson  Represents the incoming JSON object from the request that needs to be formatted into the internal entity JSON structure.
     * @param i18nResolver Represents the internationalization resolver that can be used to fetch localized strings or handle locale-specific formatting during the conversion process.
     */
    @Override
    public JsonObject format(final long orgId, final JsonObject requestJson, final I18nResolver i18nResolver) {
        final JsonObject productJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.PRODUCT.getName());

        for (final FieldInfo fieldInfo : fields) {
            final String internalName = i18nResolver.get(fieldInfo.getInternalName());

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, internalName, productJson, requestJson, i18nResolver);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case ProductFields.NAME, ProductFields.DESCRIPTION, ProductFields.SKU_NUMBER,
                         ProductFields.PRODUCT_CODE -> {
                        if (requestJson.containsKey(internalName)) {
                            productJson.put(fieldInfo.idAsString(), requestJson.getString(internalName));
                        }
                    }
                    case ProductFields.SALES_OWNER, ProductFields.QUANTITY_ORDERED, ProductFields.QUANTITY_IN_STOCK -> {
                        if (requestJson.containsKey(internalName)) {
                            productJson.put(fieldInfo.idAsString(), requestJson.getLong(internalName));
                        }
                    }
                    case ProductFields.CATEGORY, ProductFields.ACTIVE_STATUS,
                         ProductFields.TAXABLE, ProductFields.TYPE -> {
                        if (requestJson.containsKey(internalName)) {
                            mapOptionField(orgId, requestJson.get(internalName), fieldInfo, productJson, i18nResolver);
                        }
                    }
                    case ProductFields.ID -> {
                        if (requestJson.containsKey(internalName)) {
                            productJson.put(ProductFields.ID, requestJson.getLong(internalName));
                        }
                    }
                    case ProductFields.PRICE_TAGS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray jsonArray = requestJson.getJsonArray(internalName);
                            final Map<StoreSubType, FieldSubType> priceTagSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PRICE_TAGS);
                            final JsonArray array = Json.array();
                            final String priceKey = priceTagSubtypes.get(StoreSubType.UNIT_PRICE).idAsString();
                            final String currencyKey = priceTagSubtypes.get(StoreSubType.CURRENCY).idAsString();
                            final String taxKey = priceTagSubtypes.get(StoreSubType.TAX).idAsString();
                            final String discountKey = priceTagSubtypes.get(StoreSubType.DISCOUNT).idAsString();
                            final int size = jsonArray.size();

                            for (int i = 0; i < size; i++) {
                                final JsonObject priceJson = jsonArray.optJsonObject(i);

                                if (Objects.nonNull(priceJson)) {
                                    final JsonObject subFieldJson = Json.object();

                                    subFieldJson.put(priceKey, priceJson.opt(StoreSubType.UNIT_PRICE.getApiPropertyName()));
                                    subFieldJson.put(taxKey, priceJson.opt(StoreSubType.TAX.getApiPropertyName()));
                                    subFieldJson.put(discountKey, priceJson.opt(StoreSubType.DISCOUNT.getApiPropertyName()));

                                    final Object currencyValue = priceJson.opt(StoreSubType.CURRENCY.getApiPropertyName());

                                    if (TypeChecker.isNumber(currencyValue)) {
                                        subFieldJson.put(currencyKey, currencyValue);
                                    } else {
                                        subFieldJson.put(currencyKey, resolveCurrency(orgId, (String) currencyValue, i18nResolver).getId());
                                    }
                                    array.put(subFieldJson);
                                }
                            }

                            productJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case ProductFields.VALID_TILL -> {
                        if (requestJson.containsKey(internalName)) {
                            productJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(requestJson.getString(internalName)));
                        }
                    }
                }
            }
        }

        return productJson;
    }

}

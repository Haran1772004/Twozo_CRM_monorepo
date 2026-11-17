package com.tlc.crm.zapier.internal.event.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Product;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.ValueHolder;
import com.tlc.crm.entity.models.type.Option;

import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.event.ZapEventData;

import com.tlc.i18n.I18nResolver;
import com.tlc.iam.common.models.User;

import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Handles the conversion of model data into a JSON format suitable for Zapier event payloads.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductZapEventDataConverter extends AbstractZapEventDataConverter<Product> {

    private static final class Instance {
        private static final ProductZapEventDataConverter EVENT_DATA_CONVERTER = new ProductZapEventDataConverter();
    }

    public static ProductZapEventDataConverter getInstance() {
        return Instance.EVENT_DATA_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param product      The specific model instance to convert.
     * @param zapEventData The {@code ZapEventData}.
     * @return {@link JsonObject} representing the converted model data.
     */
    @Override
    protected JsonObject convert(final Product product, final ZapEventData zapEventData) {
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(product.orgId(), Source.PRODUCT.getName());
        final JsonObject productJson = Json.object();
        final I18nResolver i18nResolver = i18nService.getResolver(zapEventData.getLocale());
        final Map<Long, User> userMap = zapEventData.getGroupedUsers();
        final ValueHolder valueHolder = product.getValueHolder();

        for (final FieldInfo fieldInfo : fields) {

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(fieldInfo, valueHolder.get(fieldInfo.id()), productJson, i18nResolver, userMap);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case "name" -> productJson.put(aliasName, product.getName());
                    case "salesOwner" -> {
                        final JsonObject saleOwnerJson = Json.object();
                        final CrmUser salesOwner = product.getSalesOwner();

                        saleOwnerJson.put(NAME, userMap.get(salesOwner.getIamId()).getFullName());
                        saleOwnerJson.put(ID, salesOwner.id());

                        productJson.put(aliasName, saleOwnerJson);
                    }
                    case "category" -> {
                        final Option category = product.getCategory();

                        if (Objects.nonNull(category)) {
                            productJson.put(aliasName, i18nResolver.get(category.getOption()));
                        }
                    }
                    case "productCode" -> productJson.put(aliasName, product.getProductCode());
                    case "priceTags" -> {
                        final Collection<PriceTag> priceTags = product.getBaseCurrencyPriceTags();

                        if (Objects.nonNull(priceTags)) {
                            final JsonArray array = Json.array();

                            for (final PriceTag priceTag : priceTags) {
                                final JsonObject priceJson = Json.object();

                                priceJson.put(ZapAttributes.PRICE, priceTag.getPrice());
                                priceJson.put(ZapAttributes.TAX, priceTag.getTax());
                                priceJson.put(ZapAttributes.DISCOUNT, priceTag.getDiscount());
                                priceJson.put(ZapAttributes.CURRENCY, priceTag.getCurrency().getI18nKey());

                                array.put(priceJson);
                            }
                            productJson.put(aliasName, array);
                        }
                    }
                    case "activeStatus" -> {
                        final Option activeStatus = product.getActiveStatus();

                        if (Objects.nonNull(activeStatus)) {
                            productJson.put(aliasName, i18nResolver.get(activeStatus.getOption()));
                        }
                    }
                    case "taxable" -> {
                        final Option taxable = product.getTaxable();

                        if (Objects.nonNull(taxable)) {
                            productJson.put(aliasName, i18nResolver.get(taxable.getOption()));
                        }
                    }
                    case "quantityOrdered" -> productJson.put(aliasName, product.getQuantityOrdered());
                    case "quantityInStock" -> productJson.put(aliasName, product.getQuantityInStock());
                    case "description" -> productJson.put(aliasName, product.getDescription());
                    case "type" -> {
                        final Option type = product.getType();

                        if (Objects.nonNull(type)) {
                            productJson.put(aliasName, i18nResolver.get(type.getOption()));
                        }
                    }
                    case "skuNumber" -> productJson.put(aliasName, product.getSkuNumber());
                    case "validTill" -> {
                        final Long validTill = product.getValidTill();

                        if (Objects.nonNull(validTill)) {
                            productJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(validTill, TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                        }
                    }
                    case "updatedAt" ->
                            productJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(product.getModifiedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "lastAssignedAt" ->
                            productJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(product.getLastAssignedAt().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "createdOn" ->
                            productJson.put(aliasName, TemporalServiceProvider.get().getISOOffsetDateTimeString(product.getCreatedTime().getTimeStamp(), TimezoneInfo.UTC, ISOTemporalFormat.ISO_OFFSET_DATE_TIME));
                    case "createdBy" -> {
                        final JsonObject creatorJson = Json.object();
                        final CrmUser creator = product.getCreatedBy();

                        creatorJson.put(NAME, userMap.get(creator.getIamId()).getFullName());
                        creatorJson.put(ID, creator.id());

                        productJson.put(aliasName, creatorJson);
                    }
                    case "updatedBy" -> {
                        final JsonObject updaterJson = Json.object();
                        final CrmUser updatedBy = product.getUpdatedBy();

                        updaterJson.put(NAME, userMap.get(updatedBy.getIamId()).getFullName());
                        updaterJson.put(ID, updatedBy.id());

                        productJson.put(aliasName, updaterJson);
                    }
                }
            }
        }

        return productJson;
    }
}

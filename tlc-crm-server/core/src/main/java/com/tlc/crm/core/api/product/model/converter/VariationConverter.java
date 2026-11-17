package com.tlc.crm.core.api.product.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.crm.core.field.product.ProductFields;

import com.tlc.crm.core.api.product.manager.VariationManager;
import com.tlc.crm.core.api.product.model.PriceTag;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.crm.core.field.product.PriceTagFields;
import com.tlc.crm.core.field.product.VariationFields;

import com.tlc.crm.entity.currency.fields.CurrencyUnitFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Variation}
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class VariationConverter extends EntityConverter<Variation> {

    private final VariationManager variationManager;
    private final FieldTypeService fieldTypeService;
    private final BaseCurrencyService baseCurrencyService;
    private final MonetaryConverter monetaryConverter;

    private static class Instance {
        private static final VariationConverter INSTANCE = new VariationConverter();
    }

    private VariationConverter() {
        variationManager = VariationManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
        baseCurrencyService = BaseCurrencyService.getInstance();
        monetaryConverter = MonetaryConverter.getInstance();
    }

    /**
     * Gets the {@link VariationConverter} Instance
     *
     * @return the {@link VariationConverter}
     */
    public static VariationConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Variation}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the variation data
     * @return the {@link Variation}
     */
    @Override
    public Variation convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildVariationByPartialData(converterConfig, jsonObject);
        } else {
            return buildVariation(converterConfig, jsonObject);
        }
    }

    /**
     * Converts {@link JsonObject} into {@link Variation}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the variation data
     * @return the {@link Variation}
     */
    private Variation buildVariation(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Variation variation = new Variation();
        final String idKey = VariationFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            variation.setId(jsonObject.getLong(idKey));
        }
        setVariationData(converterConfig, variation, jsonObject);

        return variation;
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link Variation}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the variation data
     * @return the {@link Variation}
     */
    private Variation buildVariationByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Variation variation = variationManager.get(converterConfig.orgId(), jsonObject.getLong(VariationFields.ID.getName()));

        setVariationData(converterConfig, variation, jsonObject);

        return variation;
    }

    /**
     * Sets the json data into {@link Variation}
     *
     * @param converterConfig converter configuration
     * @param variation       which contains the variation data
     * @param jsonObject      which contains the variation fields data
     */
    private void setVariationData(final ConverterConfig converterConfig, final Variation variation, final JsonObject jsonObject) {
        final String variationKey = ProductFields.VARIATION;
        final String priceTagKey = ProductFields.PRICE_TAGS;
        final String productIdKey = VariationFields.PRODUCT_ID.getName();
        final Long orgId = converterConfig.orgId();

        variation.setOrgId(orgId);

        if (jsonObject.containsKey(productIdKey)) {
            variation.setProductId(jsonObject.getLong(productIdKey));
        }

        if (jsonObject.containsKey(variationKey)) {
            final JsonObject variationObject = jsonObject.optJsonObject(variationKey);

            variation.setName(variationObject.optString(ProductFields.NAME, variation.getName()));

            if (variationObject.containsKey(priceTagKey)) {
                variation.setPriceTags(buildPriceTags(orgId, variationObject.opt(VariationFields.ID.getName()),
                        variation, variationObject.optJsonArray(priceTagKey), false));
            }
        }
    }

    /**
     * Builds the {@link Variation} entity from the given json objects and map it to product
     *
     * @param orgId     Represents the organization id
     * @param productId Represents the product ID
     * @param key       Represents the key of priceTag
     * @param json      Represents the {@link JsonObject} of variation
     */
    Variation buildDefaultVariation(final Long orgId, final Long productId, final String key, final JsonObject json) {
        final Variation variation = new Variation();

        variation.setOrgId(orgId);
        variation.setProductId(productId);
        variation.setName(Variation.DEFAULT);
        variation.setPriceTags(buildPriceTags(orgId, json.opt(VariationFields.VARIATION_ID.getName()), variation, json.optJsonArray(key), true));

        return variation;
    }

    /**
     * Builds the {@link PriceTag} entity
     *
     * @param orgId         Represents the organization id
     * @param value         Represents the variation id key
     * @param variation     Represents the variation
     * @param priceTagArray Represents the {@link JsonArray} of priceTag
     * @param idAsString    Represents the idAsString
     * @return {@link Collection} of {@link PriceTag}
     */
    private Collection<PriceTag> buildPriceTags(final Long orgId, final Object value, final Variation variation,
                                                final JsonArray priceTagArray, final boolean idAsString) {
        final Collection<PriceTag> priceTags = new ArrayList<>();

        if (Objects.nonNull(priceTagArray)) {
            final Map<StoreSubType, FieldSubType> priceTagSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PRICE_TAGS);
            final String idKey = PriceTagFields.ID.getName();
            final String priceKey = idAsString ? priceTagSubtypes.get(StoreSubType.UNIT_PRICE).idAsString() : ProductFields.UNIT_PRICE;
            final String taxKey = idAsString ? priceTagSubtypes.get(StoreSubType.TAX).idAsString() : ProductFields.TAX;
            final String currencyKey = idAsString ? priceTagSubtypes.get(StoreSubType.CURRENCY).idAsString() : ProductFields.CURRENCY;
            final String discountKey = idAsString ? priceTagSubtypes.get(StoreSubType.DISCOUNT).idAsString() : ProductFields.DISCOUNT;

            for (int index = 0; index < priceTagArray.size(); index++) {
                final PriceTag priceTag = new PriceTag();
                final JsonObject priceTagJson = priceTagArray.getJsonObject(index);
                final Long variationId = value instanceof final Long id ? id : variation.id();

                if (priceTagJson.containsKey(idKey)) {
                    priceTag.setId(priceTagJson.getLong(idKey));
                }

                if (priceTagJson.containsKey(currencyKey)) {
                    final short currencyValue = priceTagJson.getShort(currencyKey);
                    final Currency currency = Currency.getCurrencyById(currencyValue);

                    priceTag.setCurrency(currency);
                }
                priceTag.setVariationId(variationId);
                priceTag.setOrgId(orgId);
                priceTag.setPrice(priceTagJson.optDouble(priceKey, Objects.nonNull(priceTag.getPrice()) ? priceTag.getPrice() : 0F));
                priceTag.setTax(priceTagJson.optDouble(taxKey, Objects.nonNull(priceTag.getTax()) ? priceTag.getTax() : 0F));
                priceTag.setDiscount(priceTagJson.optDouble(discountKey, Objects.nonNull(priceTag.getDiscount()) ? priceTag.getDiscount() : 0F));
                priceTag.setProductId(variation.getProductId());
                priceTags.add(priceTag);
                variation.setId(variationId);
            }
        }
        return priceTags;
    }

    /**
     * <p>
     * Converts {@link Variation} into {@link JsonObject}
     * </p>
     *
     * @param variation A model that holds the attributes of variation
     * @return {@link JsonObject} of variation
     */
    @Override
    public JsonObject convert(final Variation variation) {
        final JsonObject jsonObject = Json.object();
        final String idKey = VariationFields.ID.getName();

        jsonObject.put(idKey, variation.id());
        jsonObject.put(VariationFields.NAME.getName(), variation.getName());
        jsonObject.put(VariationFields.ORG_ID.getName(), variation.orgId());
        jsonObject.put(VariationFields.PRODUCT_ID.getName(), variation.getProductId());
        final Collection<PriceTag> priceTags = variation.getPriceTags();

        if (Objects.nonNull(priceTags)) {
            final JsonArray priceTagArray = Json.array();

            priceTags.forEach(priceTag -> priceTagArray.put(convertPriceTagToJson(priceTag, false)));
            jsonObject.put(VariationFields.PRICE_TAGS.getName(), priceTagArray);
        }

        return jsonObject;
    }

    /**
     * Converts the price tag to {@link JsonObject}
     *
     * @param priceTag          Represents the priceTag
     * @param isIdRequiredOnKey A flag that indicates whether the id is required on key or not.
     * @return {@link JsonObject} of priceTag
     */
    public JsonObject convertPriceTagToJson(final PriceTag priceTag, final boolean isIdRequiredOnKey) {
        final JsonObject priceTagJson = Json.object();

        loadPriceTagIntoJson(priceTag, priceTagJson, isIdRequiredOnKey, null);

        return priceTagJson;
    }

    /**
     * Loads the price tag data into the {@link JsonObject}
     *
     * @param priceTag          which contains the price tag data
     * @param priceTagJson      which is to be loaded with the price tag data
     * @param isIdRequiredOnKey A flag that indicates whether the id is required on key or not.
     */
    private void loadPriceTagIntoJson(final PriceTag priceTag, final JsonObject priceTagJson, final boolean isIdRequiredOnKey,
            final I18nResolver i18nResolver) {
        final Currency currency = priceTag.getCurrency();
        final Long orgId = priceTag.orgId();
        final BigDecimal price = BigDecimal.valueOf(priceTag.getPrice());

        priceTagJson.put(PriceTagFields.ID.getName(), priceTag.id());

        if (isIdRequiredOnKey) {
            final Map<StoreSubType, FieldSubType> priceTagSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PRICE_TAGS);

            priceTagJson.put(priceTagSubtypes.get(StoreSubType.UNIT_PRICE).idAsString(), baseCurrencyService.applyPrecision(orgId, price));
            priceTagJson.put(priceTagSubtypes.get(StoreSubType.TAX).idAsString(), priceTag.getTax());
            priceTagJson.put(priceTagSubtypes.get(StoreSubType.DISCOUNT).idAsString(), priceTag.getDiscount());

            if (Objects.nonNull(currency)) {
                priceTagJson.put(priceTagSubtypes.get(StoreSubType.CURRENCY).idAsString(), currency.getId());
            }
        } else {
            priceTagJson.put(PriceTagFields.PRICE.getName(), baseCurrencyService.applyPrecision(orgId, price));
            priceTagJson.put(PriceTagFields.TAX.getName(), priceTag.getTax());
            priceTagJson.put(PriceTagFields.DISCOUNT.getName(), priceTag.getDiscount());
            priceTagJson.put(PriceTagFields.VARIATION_ID.getName(), priceTag.getVariationId());

            if (Objects.nonNull(currency)) {
                final JsonObject currencyJson = Json.object();

                currencyJson.put(PriceTagFields.CURRENCY_ID.getName(), currency.getId());
                currencyJson.put(PriceTagFields.SYMBOL.getName(), currency.getSymbol());
                currencyJson.put(CurrencyUnitFields.CODE, currency.getCode());

                if (Objects.nonNull(i18nResolver)) {
                    currencyJson.put(FieldKeys.DISPLAY_NAME.getName(), i18nResolver.get(currency.getI18nKey()));
                }

                priceTagJson.put(PriceTagFields.CURRENCY.getName(), currencyJson);
            }
        }
    }

    /**
     * <p>
     * Converts {@link Variation} into {@link JsonObject}
     * </p>
     *
     * @param variation A model that holds the attributes of variation
     * @return {@link JsonObject} of variation
     */
    public JsonObject partialConvert(final Variation variation) {
        final JsonObject jsonObject = Json.object();
        final String idKey = VariationFields.ID.getName();

        jsonObject.put(idKey, variation.id());
        jsonObject.put(VariationFields.NAME.getName(), variation.getName());
        jsonObject.put(VariationFields.PRODUCT_ID.getName(), variation.getProductId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts {@link PriceTag} into {@link JsonObject}
     * </p>
     *
     * @param priceTag A model that holds the attributes of priceTag
     * @return {@link JsonObject} of priceTag
     */
    public JsonObject convert(final PriceTag priceTag, final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();

        loadPriceTagIntoJson(priceTag, jsonObject, false, i18nResolver);
        jsonObject.put(PriceTagFields.VARIATION_ID.getName(), priceTag.getVariationId());
        jsonObject.put(PriceTagFields.PRODUCT_ID.getName(), priceTag.getProductId());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the given {@link PriceTag} into a {@link JsonObject}.
     * </p>
     *
     * @param isIdRequiredOnKey Represent whether to include the key as id in the response or not
     * @param priceTag          Represents the {@link PriceTag} to be converted.
     * @return A {@link JsonObject} containing the details of the price tag.
     */
    public JsonObject convertPriceTagToJson(final boolean isIdRequiredOnKey, final PriceTag priceTag) {
        final Currency currency = priceTag.getCurrency();
        final Long orgId = priceTag.orgId();
        final BigDecimal price = BigDecimal.valueOf(priceTag.getPrice());
        final JsonObject priceTagJson = Json.object();

        priceTagJson.put(PriceTagFields.ID.getName(), priceTag.id());

        if (isIdRequiredOnKey) {
            final Map<StoreSubType, FieldSubType> priceTagSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PRICE_TAGS);

            priceTagJson.put(priceTagSubtypes.get(StoreSubType.UNIT_PRICE).idAsString(), baseCurrencyService.applyPrecision(orgId, price));
            priceTagJson.put(priceTagSubtypes.get(StoreSubType.TAX).idAsString(), priceTag.getTax());
            priceTagJson.put(priceTagSubtypes.get(StoreSubType.DISCOUNT).idAsString(), priceTag.getDiscount());

            if (Objects.nonNull(currency)) {
                priceTagJson.put(priceTagSubtypes.get(StoreSubType.CURRENCY).idAsString(), monetaryConverter.convert(currency));
            }
        } else {
            priceTagJson.put(PriceTagFields.PRICE.getName(), baseCurrencyService.applyPrecision(orgId, price));
            priceTagJson.put(PriceTagFields.TAX.getName(), priceTag.getTax());
            priceTagJson.put(PriceTagFields.DISCOUNT.getName(), priceTag.getDiscount());
            priceTagJson.put(PriceTagFields.VARIATION_ID.getName(), priceTag.getVariationId());

            if (Objects.nonNull(currency)) {
                final JsonObject currencyJson = Json.object();

                currencyJson.put(PriceTagFields.CURRENCY_ID.getName(), currency.getId());
                currencyJson.put(PriceTagFields.SYMBOL.getName(), currency.getSymbol());

                priceTagJson.put(PriceTagFields.CURRENCY.getName(), currencyJson);
            }
        }

        return priceTagJson;
    }

    /**
     * <p>
     * Converts the given {@link Variation} into a {@link JsonObject}.
     * </p>
     *
     * @param variation       A {@link Variation} model that holds the attributes to be converted.
     * @return A {@link JsonObject} containing the details of the variation.
     */
    public JsonObject convertModelToJson(final Variation variation) {
        final JsonObject jsonObject = Json.object();
        final String idKey = VariationFields.ID.getName();

        jsonObject.put(idKey, variation.id());
        jsonObject.put(VariationFields.NAME.getName(), variation.getName());
        jsonObject.put(VariationFields.ORG_ID.getName(), variation.orgId());
        jsonObject.put(VariationFields.PRODUCT_ID.getName(), variation.getProductId());
        final Collection<PriceTag> priceTags = variation.getPriceTags();

        if (Objects.nonNull(priceTags)) {
            final JsonArray priceTagArray = Json.array();

            priceTags.forEach(priceTag -> priceTagArray.put(convertPriceTagToJson(false, priceTag)));
            jsonObject.put(VariationFields.PRICE_TAGS.getName(), priceTagArray);
        }

        return jsonObject;
    }
}

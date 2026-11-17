package com.tlc.plans.and.billings.models.converter;

import com.tlc.commons.converter.ModelConverter;
import com.tlc.commons.converter.model.ConverterData;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.exchange.rate.service.ExchangeRateService;
import com.tlc.exchange.rate.service.provider.ExchangeRateServiceProvider;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.plans.and.billings.fields.SubscriptionPlanFields;
import com.tlc.plans.and.billings.models.BillingCycle;
import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.Discounts;
import com.tlc.plans.and.billings.models.Feature;
import com.tlc.plans.and.billings.models.FeatureGroup;
import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.PricingBasis;
import com.tlc.plans.and.billings.models.PricingScheme;
import com.tlc.plans.and.billings.models.SubscriptionPlan;

import java.math.BigDecimal;
import java.math.MathContext;

import java.util.Collection;
import java.util.Objects;

/**
 * Converter for {@link SubscriptionPlan} domain objects to and from their JSON representations.
 * <p>
 * Handles transformation of all complex and nested objects, supporting both
 * serialization (output to JSON) and deserialization (reading from JSON).
 * Integrates with internationalization services to display correct field values.
 * <p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SubscriptionPlanConverter implements ModelConverter<SubscriptionPlan, JsonObject> {

    private static final short HUNDRED = 100;

    private final I18nService i18nService;
    private final ExchangeRateService exchangeRateService;

    private SubscriptionPlanConverter() {
        i18nService = I18nServiceProvider.get();
        exchangeRateService = ExchangeRateServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final SubscriptionPlanConverter SUBSCRIPTION_PLAN_CONVERTER = new SubscriptionPlanConverter();
    }

    /**
     * Returns the singleton instance of this converter.
     *
     * @return the SubscriptionPlanConverter singleton
     */
    public static SubscriptionPlanConverter getInstance() {
        return InstanceHolder.SUBSCRIPTION_PLAN_CONVERTER;
    }

    /**
     * Converts a JSON representation into a fully populated {@link SubscriptionPlan} domain object.
     *
     * @param converterData container object wrapping the {@link JsonObject} to be converted
     *                      containing subscription plan data in JSON format
     * @return a {@link SubscriptionPlan} instance populated with data from the input JSON
     */
    @Override
    public <D extends ConverterData<JsonObject>> SubscriptionPlan convertToModel(final D converterData) {
        final JsonObject jsonObject = converterData.dataToBeConverted();
        final SubscriptionPlan plan = new SubscriptionPlan();

        if (jsonObject.containsKey(SubscriptionPlanFields.ID)) {
            plan.setId(jsonObject.getLong(SubscriptionPlanFields.ID));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.NAME)) {
            plan.setName(jsonObject.getString(SubscriptionPlanFields.NAME));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.DESCRIPTION)) {
            plan.setDescription(jsonObject.getString(SubscriptionPlanFields.DESCRIPTION));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.SIMPLIFIED_DESCRIPTION)) {
            plan.setSimplifiedDescription(jsonObject.getString(SubscriptionPlanFields.SIMPLIFIED_DESCRIPTION));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.PLAN_TYPE)) {
            plan.setPlanType(PlanType.get(jsonObject.getShort(SubscriptionPlanFields.PLAN_TYPE)));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.FEATURE_GROUPS)) {
            final JsonArray featureGroupArray = jsonObject.getJsonArray(SubscriptionPlanFields.FEATURE_GROUPS);
            final int size = featureGroupArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject featureGroupJson = featureGroupArray.getJsonObject(index);

                plan.addFeatureGroup(convertJsonIntoFeatureGroup(featureGroupJson));
            }
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.KEY_FEATURES)) {
            final JsonArray keyFeaturesArray = jsonObject.getJsonArray(SubscriptionPlanFields.KEY_FEATURES);
            final int size = keyFeaturesArray.size();

            for (int index = 0; index < size; index++) {
                final Feature keyFeature = new Feature();

                keyFeature.setName(new I18nKey(keyFeaturesArray.getString(index)));
                plan.addKeyFeature(keyFeature);
            }
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.PRICE)) {
            final JsonObject priceJson = jsonObject.getJsonObject(SubscriptionPlanFields.PRICE);

            plan.setPricingScheme(convertJsonIntoPricingScheme(priceJson));
        }

        if (jsonObject.containsKey(SubscriptionPlanFields.ACTIVE_STATUS)) {
            plan.setActive(jsonObject.getBoolean(SubscriptionPlanFields.ACTIVE_STATUS));
        }

        return plan;
    }

    /**
     * Converts a {@link SubscriptionPlan} domain object to a localized JSON representation.
     * Ensures all string fields are translated, and all nested objects are recursively serialized.
     *
     * @param converterData container object wrapping the {@link SubscriptionPlan} to be converted
     *                      containing subscription plan data
     * @return the JSON representation of the subscription plan
     */
    @Override
    public <D extends ConverterData<SubscriptionPlan>> JsonObject convertToJson(final D converterData) {
        final SubscriptionPlan plan = converterData.dataToBeConverted();
        final JsonObject planJson = Json.object();
        final I18nKey description = plan.getDescription();
        final Collection<Feature> keyFeatures = plan.getKeyFeatures();
        final I18nResolver i18nResolver = i18nService.getResolver(converterData.locale());

        planJson.put(SubscriptionPlanFields.ID, plan.getId());
        planJson.put(SubscriptionPlanFields.NAME, i18nResolver.get(plan.getName()));

        if (Objects.nonNull(description)) {
            planJson.put(SubscriptionPlanFields.DESCRIPTION, i18nResolver.get(description));
        }

        if (Objects.nonNull(keyFeatures) && !keyFeatures.isEmpty()) {
            planJson.put(SubscriptionPlanFields.KEY_FEATURES, convertKeyFeatureIntoJson(keyFeatures, i18nResolver));
        }

        planJson.put(SubscriptionPlanFields.PLAN_TYPE, plan.getPlanType().getId());
        planJson.put(SubscriptionPlanFields.PRICE, convertPricingSchemeIntoJson(plan.getPricingScheme(), i18nResolver));

        return planJson;
    }

    /**
     * Serializes a collection of feature groups into a localized JSON array.
     *
     * @param featureGroups feature groups to include
     * @param i18nResolver  internationalization resolver
     * @return JSON array of feature groups
     */
    public JsonArray convertFeatureGroupIntoJson(final Collection<FeatureGroup> featureGroups, final I18nResolver i18nResolver) {
        final JsonArray featureGroupArray = Json.array();

        for (final FeatureGroup featureGroup : featureGroups) {
            featureGroupArray.put(convertFeatureGroupIntoJson(featureGroup, i18nResolver));
        }

        return featureGroupArray;
    }

    /**
     * Serializes a collection of key features into a localized JSON array.
     *
     * @param keyFeatures  the features to include
     * @param i18nResolver internationalization resolver
     * @return serialized JSON array of key features
     */
    private JsonArray convertKeyFeatureIntoJson(final Collection<Feature> keyFeatures, final I18nResolver i18nResolver) {
        final JsonArray keyFeaturesArray = Json.array();

        for (final Feature keyFeature : keyFeatures) {
            keyFeaturesArray.put(i18nResolver.get(keyFeature.getName()));
        }

        return keyFeaturesArray;
    }

    /**
     * Serializes a single feature group into a localized JSON object.
     *
     * @param featureGroup feature group to render
     * @param i18nResolver i18n translation resolver
     * @return JSON object for the feature group
     */
    private JsonObject convertFeatureGroupIntoJson(final FeatureGroup featureGroup, final I18nResolver i18nResolver) {
        final JsonObject featureGroupJson = Json.object();

        featureGroupJson.put(SubscriptionPlanFields.NAME, i18nResolver.get(featureGroup.getName()));
        featureGroupJson.put(SubscriptionPlanFields.FEATURES, convertFeatureIntoJson(featureGroup.getFeatures(), i18nResolver));

        return featureGroupJson;
    }

    /**
     * Serializes a collection of features into a localized JSON array.
     *
     * @param features     the features to render
     * @param i18nResolver i18n translation resolver
     * @return JSON array containing serialized features
     */
    private JsonArray convertFeatureIntoJson(final Collection<Feature> features, final I18nResolver i18nResolver) {
        final JsonArray featureArray = Json.array();

        for (final Feature feature : features) {
            featureArray.put(convertFeatureIntoJson(feature, i18nResolver));
        }

        return featureArray;
    }

    /**
     * Serializes a single feature into a localized JSON object, including config fields if present.
     *
     * @param feature      the feature object to serialize
     * @param i18nResolver resolver for i18n translations
     * @return feature as a JSON object
     */
    private JsonObject convertFeatureIntoJson(final Feature feature, final I18nResolver i18nResolver) {
        final JsonObject featureJson = Json.object();
        final Feature.Config config = feature.getConfig();

        featureJson.put(SubscriptionPlanFields.NAME, i18nResolver.get(feature.getName()));
        featureJson.put(SubscriptionPlanFields.DESCRIPTION, i18nResolver.get(feature.getDescription()));

        if (Objects.nonNull(config)) {
            featureJson.put(SubscriptionPlanFields.CONFIG, config.toJson());
        }

        return featureJson;
    }

    /**
     * Serializes a PricingScheme domain object (with discounts) into a JSON object.
     *
     * @param pricingScheme the pricing scheme to serialize
     * @param i18nResolver  i18n resolver for translation
     * @return pricing scheme as JSON object
     */
    public JsonObject convertPricingSchemeIntoJson(final PricingScheme pricingScheme,
                                                   final I18nResolver i18nResolver) {
        final JsonObject priceJson = Json.object();
        final Discounts discounts = pricingScheme.getDiscounts();
        final Currency baseCurrency = pricingScheme.getBaseCurrency();
        final double planPrice = pricingScheme.getValue();
        final double planValue;

        if (Currency.USD == baseCurrency) {
            planValue = planPrice;
        } else {
            final BigDecimal value = BigDecimal.valueOf(planPrice);
            final BigDecimal exchangeRate = exchangeRateService.getExchangeRate(Currency.USD, baseCurrency);

            planValue = value.multiply(exchangeRate, MathContext.DECIMAL32).doubleValue();
        }
        priceJson.put(SubscriptionPlanFields.ID, pricingScheme.getId());
        priceJson.put(SubscriptionPlanFields.VALUE, String.format("%s%s", baseCurrency.getSymbol(), Math.round(planValue * 10.0) / 10.0));
        priceJson.put(SubscriptionPlanFields.PRICING_BASIS, pricingScheme.getPricingBasis().getId());
        priceJson.put(SubscriptionPlanFields.BILLING_CYCLE, pricingScheme.getBillingCycle().getId());

        if (Objects.nonNull(discounts)) {
            priceJson.put(SubscriptionPlanFields.DISCOUNTED_VALUE, discounts.getDiscountedValue(planValue));
            priceJson.put(SubscriptionPlanFields.DISCOUNTS, convertDiscountIntoJson(discounts, i18nResolver));
        }

        return priceJson;
    }

    /**
     * Serializes a collection of discounts to JSON.
     *
     * @param discounts    discounts to include
     * @param i18nResolver i18n resolver for translation
     */
    private JsonArray convertDiscountIntoJson(final Discounts discounts, final I18nResolver i18nResolver) {
        final JsonArray discountArray = Json.array();

        for (final Discount discount : discounts.getAll()) {
            discountArray.put(convertDiscountIntoJson(discount, i18nResolver));
        }

        return discountArray;
    }

    /**
     * Serializes a Discount domain object into JSON.
     *
     * @param discount     discount object to serialize
     * @param i18nResolver i18n resolver for translation
     * @return discount as JSON object
     */
    public JsonObject convertDiscountIntoJson(final Discount discount, final I18nResolver i18nResolver) {
        final JsonObject discountJson = Json.object();
        final DateTime startTime = discount.getStartTime();
        final DateTime expirationTime = discount.getExpirationTime();

        discountJson.put(SubscriptionPlanFields.ID, discount.getId());
        discountJson.put(SubscriptionPlanFields.REFERENCE_ID, discount.getReferenceId());
        discountJson.put(SubscriptionPlanFields.NAME, i18nResolver.get(discount.getName()));
        discountJson.put(SubscriptionPlanFields.DESCRIPTION, i18nResolver.get(discount.getDescription()));
        discountJson.put(SubscriptionPlanFields.VALUE, BigDecimal.valueOf(0.2 * HUNDRED).stripTrailingZeros().toPlainString());

        if (Objects.nonNull(startTime)) {
            discountJson.put(SubscriptionPlanFields.START_TIME, startTime.getUtcTimeStamp());
        }

        if (Objects.nonNull(expirationTime)) {
            discountJson.put(SubscriptionPlanFields.EXPIRATION_TIME, expirationTime.getUtcTimeStamp());
        }

        return discountJson;
    }

    /**
     * Parses a JSON object to a PricingScheme domain object (with discounts).
     *
     * @param priceJson the JSON containing pricing data
     * @return parsed PricingScheme object
     */
    private PricingScheme convertJsonIntoPricingScheme(final JsonObject priceJson) {
        final PricingScheme pricingScheme = new PricingScheme();

        if (priceJson.containsKey(SubscriptionPlanFields.ID)) {
            pricingScheme.setId(priceJson.getLong(SubscriptionPlanFields.ID));
        }

        if (priceJson.containsKey(SubscriptionPlanFields.VALUE)) {
            pricingScheme.setValue(priceJson.getDouble(SubscriptionPlanFields.VALUE));
        }

        if (priceJson.containsKey(SubscriptionPlanFields.PRICING_BASIS)) {
            pricingScheme.setPricingBasis(PricingBasis.get(priceJson.getShort(SubscriptionPlanFields.PRICING_BASIS)));
        }

        if (priceJson.containsKey(SubscriptionPlanFields.BILLING_CYCLE)) {
            pricingScheme.setBillingCycle(BillingCycle.get(priceJson.getShort(SubscriptionPlanFields.BILLING_CYCLE)));
        }

        if (priceJson.containsKey(SubscriptionPlanFields.DISCOUNTS)) {
            final JsonArray discountArray = priceJson.getJsonArray(SubscriptionPlanFields.DISCOUNTS);
            final int size = discountArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject discountJson = discountArray.getJsonObject(index);

                pricingScheme.addDiscount(convertJsonIntoDiscount(discountJson));
            }
        }

        return pricingScheme;
    }

    /**
     * Parses a JSON object representing a discount into a Discount domain object.
     *
     * @param discountJson JSON object with discount data
     * @return constructed Discount object
     */
    private Discount convertJsonIntoDiscount(final JsonObject discountJson) {
        final Discount discount = new Discount();

        if (discountJson.containsKey(SubscriptionPlanFields.ID)) {
            discount.setId(discountJson.getLong(SubscriptionPlanFields.ID));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.NAME)) {
            discount.setName(new I18nKey(discountJson.getString(SubscriptionPlanFields.NAME)));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.REFERENCE_ID)) {
            discount.setReferenceId(discountJson.getString(SubscriptionPlanFields.REFERENCE_ID));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.DESCRIPTION)) {
            discount.setDescription(new I18nKey(discountJson.getString(SubscriptionPlanFields.DESCRIPTION)));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.VALUE)) {
            discount.setValue(discountJson.getDouble(SubscriptionPlanFields.VALUE));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.START_TIME)) {
            discount.setStartTime(new DateTime(discountJson.getLong(SubscriptionPlanFields.START_TIME)));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.EXPIRATION_TIME)) {
            discount.setExpirationTime(new DateTime(discountJson.getLong(SubscriptionPlanFields.EXPIRATION_TIME)));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.EXPIRATION_PERIOD)) {
            discount.setExpirationPeriod(discountJson.getString(SubscriptionPlanFields.EXPIRATION_PERIOD));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.MAX_APPLY_LIMIT)) {
            discount.setMaxApplyLimit(discountJson.getInt(SubscriptionPlanFields.MAX_APPLY_LIMIT));
        }

        if (discountJson.containsKey(SubscriptionPlanFields.ACTIVE_STATUS)) {
            discount.setActive(discountJson.getBoolean(SubscriptionPlanFields.ACTIVE_STATUS));
        }

        return discount;
    }

    /**
     * Converts a JSON object into a FeatureGroup domain object.
     *
     * @param featureGroupJson input JSON for feature group
     * @return FeatureGroup domain object
     */
    private FeatureGroup convertJsonIntoFeatureGroup(final JsonObject featureGroupJson) {
        final FeatureGroup featureGroup = new FeatureGroup();

        featureGroup.setId(featureGroupJson.getLong(SubscriptionPlanFields.ID));

        if (featureGroupJson.containsKey(SubscriptionPlanFields.FEATURES)) {
            final JsonArray featureArray = featureGroupJson.getJsonArray(SubscriptionPlanFields.FEATURES);
            final int size = featureArray.size();

            for (int index = 0; index < size; index++) {
                final JsonObject featureJson = featureArray.getJsonObject(index);

                featureGroup.addFeature(convertJsonIntoFeature(featureJson, featureGroup));
            }
        }

        return featureGroup;
    }

    /**
     * Converts a feature JSON into a Feature object, associating it with a FeatureGroup.
     *
     * @param featureJson  JSON for the feature
     * @param featureGroup parent feature group
     * @return Feature object
     */
    private Feature convertJsonIntoFeature(final JsonObject featureJson, final FeatureGroup featureGroup) {
        final Feature feature = new Feature();

        feature.setId(featureJson.getLong(SubscriptionPlanFields.ID));
        feature.setFeatureGroup(featureGroup);

        return feature;
    }
}
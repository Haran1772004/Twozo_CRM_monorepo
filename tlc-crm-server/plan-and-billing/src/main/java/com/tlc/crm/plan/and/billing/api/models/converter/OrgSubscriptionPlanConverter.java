package com.tlc.crm.plan.and.billing.api.models.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.CrmConverterData;
import com.tlc.crm.common.converter.CrmModelConverter;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionStatus;
import com.tlc.crm.plan.and.billing.calculator.PlanExpirationCalculator;
import com.tlc.crm.plan.and.billing.fields.SubscriptionPlanFields;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.PricingScheme;
import com.tlc.plans.and.billings.models.SubscriptionPlan;
import com.tlc.plans.and.billings.models.converter.SubscriptionPlanConverter;

import java.util.Objects;

/**
 * Converter class for transforming between {@link OrgSubscriptionPlan}/
 * {@link OrgSubscriptions} domain models and their JSON representations
 * for use in APIs and UI flows.
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgSubscriptionPlanConverter extends CrmModelConverter<OrgSubscriptionPlan, JsonObject> {

    private final I18nService i18nService;
    private final OrgPlanManager orgPlanManager;
    private final SubscriptionPlanConverter subscriptionPlanConverter;

    private OrgSubscriptionPlanConverter() {
        i18nService = I18nServiceProvider.get();
        orgPlanManager = OrgPlanManager.getInstance();
        subscriptionPlanConverter = SubscriptionPlanConverter.getInstance();
    }

    private static final class InstanceHolder {
        private static final OrgSubscriptionPlanConverter SUBSCRIPTION_PLAN_CONVERTER = new OrgSubscriptionPlanConverter();
    }

    /**
     * Returns the singleton instance of this converter.
     *
     * @return instance of OrgSubscriptionPlanConverter
     */
    public static OrgSubscriptionPlanConverter getInstance() {
        return InstanceHolder.SUBSCRIPTION_PLAN_CONVERTER;
    }

    /**
     * Converts a given {@link OrgSubscriptionPlan} instance into JSON for API/response use,
     * including resolved translation fields and computed pricing blocks.
     *
     * @param converterData container object wrapping the {@link SubscriptionPlan} to be converted
     *                      containing subscription plan data
     * @return the JSON representation of the subscription plan
     */
    @Override
    public JsonObject convertToJson(final CrmConverterData<OrgSubscriptionPlan> converterData) {
        final OrgSubscriptionPlan orgSubscriptionPlan = converterData.dataToBeConverted();
        final JsonObject planJson = Json.object();
        final SubscriptionPlan plan = orgSubscriptionPlan.getPlan();
        final I18nKey simplifiedDescription = plan.getSimplifiedDescription();
        final I18nResolver i18nResolver = i18nService.getResolver(converterData.locale());
        final DateTime expirationTime = orgSubscriptionPlan.getExpirationTime();

        planJson.put(SubscriptionPlanFields.ID, orgSubscriptionPlan.id());
        planJson.put(SubscriptionPlanFields.NAME, i18nResolver.get(plan.getName()));
        planJson.put(SubscriptionPlanFields.PLAN_ID, plan.getId());
        planJson.put(SubscriptionPlanFields.ORDER, plan.getOrder());
        planJson.put(SubscriptionPlanFields.QUANTITY, orgSubscriptionPlan.getQuantity());
        planJson.put(SubscriptionPlanFields.PRICE, subscriptionPlanConverter.convertPricingSchemeIntoJson(plan.getPricingScheme(), i18nResolver));
        planJson.put(SubscriptionPlanFields.SUBSCRIPTION_STATUS, orgSubscriptionPlan.getStatus().getId());

        if (Objects.nonNull(expirationTime)) {
            planJson.put(SubscriptionPlanFields.REMAINING_DAYS, PlanExpirationCalculator.calculateRemainingDays(expirationTime));
        }

        if (Objects.nonNull(simplifiedDescription)) {
            planJson.put(SubscriptionPlanFields.DESCRIPTION, i18nResolver.get(simplifiedDescription));
        }

        return planJson;
    }

    /**
     * Builds an {@link OrgSubscriptionPlan} from input JSON and converter configuration,
     * including all nested objects, dates, and links.
     *
     * @param converterData container object wrapping the {@link JsonObject} to be converted
     *                      containing org subscription plan data in JSON format
     * @return a {@link SubscriptionPlan} instance populated with data from the input JSON
     */
    @Override
    public OrgSubscriptionPlan convertToModel(final CrmConverterData<JsonObject> converterData) {
        final JsonObject planJson = converterData.dataToBeConverted();
        final Long id = planJson.containsKey(SubscriptionPlanFields.ID) ? planJson.getLong(SubscriptionPlanFields.ID) : null;
        final OrgSubscriptionPlan orgSubscriptionPlan = getOrgSubscriptionPlan(converterData.orgId(), id, converterData.isUpdateAction());

        orgSubscriptionPlan.setId(id);
        orgSubscriptionPlan.setOrgId(converterData.orgId());

        if (planJson.containsKey(SubscriptionPlanFields.QUANTITY)) {
            orgSubscriptionPlan.setQuantity(planJson.getInt(SubscriptionPlanFields.QUANTITY));
        }

        if (planJson.containsKey(SubscriptionPlanFields.PLAN_ID)) {
            final SubscriptionPlan subscriptionPlan = new SubscriptionPlan();

            subscriptionPlan.setId(planJson.getLong(SubscriptionPlanFields.PLAN_ID));

            if (planJson.containsKey(SubscriptionPlanFields.NAME)) {
                subscriptionPlan.setName(planJson.getString(SubscriptionPlanFields.NAME));
            }

            if (planJson.containsKey(SubscriptionPlanFields.PRICE_ID)) {
                final PricingScheme pricingScheme = new PricingScheme();

                pricingScheme.setId(planJson.getLong(SubscriptionPlanFields.PRICE_ID));
                subscriptionPlan.setPricingScheme(pricingScheme);
            }

            if (planJson.containsKey(SubscriptionPlanFields.ORDER)) {
                subscriptionPlan.setOrder(planJson.getShort(SubscriptionPlanFields.ORDER));
            }
            orgSubscriptionPlan.setPlan(subscriptionPlan);
        }

        if (planJson.containsKey(SubscriptionPlanFields.DISCOUNTS)) {
            final JsonArray discountArray = planJson.getJsonArray(SubscriptionPlanFields.DISCOUNTS);
            final int size = discountArray.size();

            for (int index = 0; index < size; index++) {
                final Discount discount = new Discount();

                discount.setReferenceId(discountArray.getString(index));
                orgSubscriptionPlan.addDiscount(discount);
            }
        }

        if (planJson.containsKey(SubscriptionPlanFields.SUBSCRIPTION_STATUS)) {
            orgSubscriptionPlan.setStatus(SubscriptionStatus.get(planJson.getShort(SubscriptionPlanFields.SUBSCRIPTION_STATUS)));
        }

        return orgSubscriptionPlan;
    }

    /**
     * Returns a new or loaded {@link OrgSubscriptionPlan} based on context and existing ID.
     *
     * @param orgId          organization ID to bind to the plan
     * @param id             plan ID, or null for new
     * @param isUpdateAction whether the action is update or not
     * @return OrgSubscriptionPlan suitable for population
     */
    private OrgSubscriptionPlan getOrgSubscriptionPlan(final long orgId, final Long id, final boolean isUpdateAction) {
        return isUpdateAction && Objects.nonNull(id) ? orgPlanManager.get(orgId, id) : new OrgSubscriptionPlan();
    }
}
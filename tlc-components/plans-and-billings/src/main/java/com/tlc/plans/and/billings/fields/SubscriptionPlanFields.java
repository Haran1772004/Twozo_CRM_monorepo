package com.tlc.plans.and.billings.fields;

/**
 * Constants for all field names used in subscription plan API models and data flows.
 *
 * <p>
 * Centralizes JSON key strings and field identifiers used in serialization,
 * deserialization, validation, and client/server message contracts for the
 * Plans & Billings module. Ensures consistency and prevents typographical
 * errors when referencing fields throughout the application.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class SubscriptionPlanFields {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String PLAN_TYPE = "planType";
    public static final String ACTIVE_STATUS = "activeStatus";
    public static final String FEATURE_GROUPS = "featureGroups";
    public static final String FEATURES = "features";
    public static final String IS_ENABLED = "isEnabled";
    public static final String ACCESSIBLE_COUNT = "accessibleCount";
    public static final String PRICE = "price";
    public static final String PRICING_BASIS = "pricingBasis";
    public static final String BILLING_CYCLE = "billingCycle";
    public static final String VALUE = "value";
    public static final String DISCOUNTS = "discounts";
    public static final String REFERENCE_ID = "referenceId";
    public static final String START_TIME = "startTime";
    public static final String EXPIRATION_TIME = "expirationTime";
    public static final String MAX_APPLY_LIMIT = "maxApplyLimit";
    public static final String PLAN_ID = "planId";
    public static final String SIMPLIFIED_DESCRIPTION = "simplifiedDescription";
    public static final String KEY_FEATURES = "keyFeatures";
    public static final String PLAN_CONFIG = "planConfig";
    public static final String SUPPORTING_HOURS = "supportingHours";
    public static final String SUPPORTING_DAYS = "supportingDays";
    public static final String CONFIG = "config";
    public static final String DISCOUNTED_VALUE = "discountedValue";
    public static final String EXPIRATION_PERIOD = "expirationPeriod";

    private SubscriptionPlanFields() {
    }
}

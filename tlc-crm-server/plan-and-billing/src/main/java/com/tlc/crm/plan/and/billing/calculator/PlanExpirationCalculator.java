package com.tlc.crm.plan.and.billing.calculator;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.plans.and.billings.models.BillingCycle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for calculating plan expiration dates based on the start date and billing cycle.
 * Supports monthly and yearly billing cycles.
 *
 * @author Sathish
 * @version 1.0
 */
public final class PlanExpirationCalculator {

    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    private static final short ONE = 1;

    private PlanExpirationCalculator() {
    }

    /**
     * Calculates the expiration date and time for a billing plan, given the plan start time and cycle type.
     * The date is returned in UTC and accommodates monthly or yearly cycles.
     *
     * @param startTime    the start time of the billing plan (as a DateTime object)
     * @param billingCycle the billing cycle type (MONTHLY or YEARLY)
     * @return a DateTime object representing the calculated expiration date/time
     */
    public static DateTime calculate(final DateTime startTime, final BillingCycle billingCycle) {
        final Instant instant = Instant.ofEpochMilli(startTime.getUtcTimeStamp());

        return new DateTime(switch (billingCycle) {
            case MONTHLY -> instant.atZone(UTC_ZONE_ID).plusMonths(ONE).toInstant().toEpochMilli();
            case YEARLY -> instant.atZone(UTC_ZONE_ID).plusYears(ONE).toInstant().toEpochMilli();
        });
    }

    /**
     * Calculates the remaining number of days in a subscription plan from now to expiration.
     *
     * @param expirationTime the planned expiration date
     * @return number of days remaining, or 0 if expired
     */
    public static int calculateRemainingDays(final DateTime expirationTime) {
        final Instant currentTime = Instant.now();
        final Instant expiryTime = Instant.ofEpochMilli(expirationTime.getUtcTimeStamp());

        return Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(currentTime, expiryTime)));
    }
}

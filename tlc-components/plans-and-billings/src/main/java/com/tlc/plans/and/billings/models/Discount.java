package com.tlc.plans.and.billings.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.i18n.I18nKey;

import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.Period;

import com.tlc.plans.and.billings.internal.status.SubscriptionErrorCodes;

import java.time.temporal.ChronoUnit;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a discount that can be applied to subscription plans.
 * Includes metadata such as name, description, duration, value, and usage limits.
 *
 * @author Sathish
 * @version 1.0
 */
public class Discount {

    private Long id;
    private I18nKey name;
    private I18nKey description;
    private I18nKey simplifiedDescription;
    private String referenceId;
    private Type type;
    private double value;
    private DateTime startTime;
    private DateTime expirationTime;
    private Integer maxApplyLimit;
    private String expirationPeriod;
    private boolean isActive;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public I18nKey getName() {
        return name;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public I18nKey getDescription() {
        return description;
    }

    public void setDescription(final I18nKey description) {
        this.description = description;
    }

    public I18nKey getSimplifiedDescription() {
        return simplifiedDescription;
    }

    public void setSimplifiedDescription(final I18nKey simplifiedDescription) {
        this.simplifiedDescription = simplifiedDescription;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }

    public Integer getMaxApplyLimit() {
        return maxApplyLimit;
    }

    public void setMaxApplyLimit(final Integer maxApplyLimit) {
        this.maxApplyLimit = maxApplyLimit;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(final DateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public double getValue() {
        return value;
    }

    public void setValue(final double value) {
        this.value = value;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(final String referenceId) {
        this.referenceId = referenceId;
    }

    public void setExpirationPeriod(final String expirationPeriod) {
        this.expirationPeriod = expirationPeriod;
    }

    public Period getExpirationPeriod() {
        return Objects.isNull(expirationPeriod) ? null : buildPeriod(expirationPeriod);
    }

    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final Discount discount = (Discount) object;

        return Objects.equals(id, discount.id) && Objects.equals(referenceId, discount.referenceId) && type == discount.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, referenceId, type);
    }

    public enum Type {

        CUSTOM_DISCOUNT((short) 1),
        YEARLY_PLAN_DISCOUNT((short) 2),
        CANCELLATION_DISCOUNT((short) 3),
        ;

        private final short id;

        Type(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static Type get(final short id) {
            return switch (id) {
                case 1 -> CUSTOM_DISCOUNT;
                case 2 -> YEARLY_PLAN_DISCOUNT;
                case 3 -> CANCELLATION_DISCOUNT;
                default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_DISCOUNT_TYPE);
            };
        }

        public static Collection<Short> getDefaultDiscountTypeIds() {
            return Set.of(YEARLY_PLAN_DISCOUNT.id);
        }
    }

    /**
     * Parses a time period string into a {@link Period} object containing a value and a unit.
     * <p>
     * This method expects a space-separated string format (e.g., "30 day", "1 week", "6 month").
     * If a unit is not explicitly provided in the second part of the string, it defaults to
     * {@link ChronoUnit#MONTHS}. Supported units are "day", "week", "month", and "year".
     * </p>
     *
     * @param timePeriod A space-delimited string where the first part is a numeric value and the optional second part is the time unit.
     * @return A {@link Period} object representing the parsed duration and {@link ChronoUnit}.
     */
    private Period buildPeriod(final String timePeriod) {
        final String[] timeUnitAndValue = timePeriod.trim().split(StringUtil.Delimiter.EMPTY_SPACE.getDelimiter());
        final DateInterval dateInterval;

        if (timeUnitAndValue.length == NumericalConstant.TWO) {
            dateInterval = switch (timeUnitAndValue[1].toLowerCase()) {
                case "day", "days" -> DateInterval.DAYS;
                case "week", "weeks" -> DateInterval.WEEKS;
                case "month", "months" -> DateInterval.MONTHS;
                case "year", "years" -> DateInterval.YEARS;
                default -> throw ErrorCode.get(SubscriptionErrorCodes.INVALID_TIME_PERIOD, String.format("Time period : %s", timePeriod));
            };
        } else {
            dateInterval = DateInterval.MONTHS;
        }
        return new Period(Integer.parseInt(timeUnitAndValue[0]), dateInterval);
    }
}

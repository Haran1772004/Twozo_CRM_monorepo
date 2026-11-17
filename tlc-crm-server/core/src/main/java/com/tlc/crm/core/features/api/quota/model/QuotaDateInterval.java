package com.tlc.crm.core.features.api.quota.model;

import com.tlc.crm.core.features.api.quota.fields.QuotaFields;
import com.tlc.l10n.temporal.interval.DateInterval;

/**
 * Model that represents the {@link QuotaDateInterval}
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaDateInterval {

    private final short id;

    private final DateInterval dateInterval;

    public QuotaDateInterval(final short id, final DateInterval dateInterval) {
        this.dateInterval = dateInterval;
        this.id = id;
    }

    public DateInterval getDateInterval() {
        return dateInterval;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the name representation of the date interval.
     * </p>
     *
     * @return The interval name based on the date interval.
     */
    public String getIntervalName() {
        return switch (dateInterval) {
            case MONTHS -> QuotaFields.MONTH;
            case QUARTERS -> QuotaFields.QUARTER;
            default -> QuotaFields.YEAR;
        };
    }
}

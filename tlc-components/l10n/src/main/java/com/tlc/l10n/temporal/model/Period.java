package com.tlc.l10n.temporal.model;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.service.TemporalService;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * A data record representing a temporal duration consisting of a numeric value
 * and a {@link DateInterval}.
 * <p>
 * This record provides utility methods to calculate future or past timestamps
 * in UTC milliseconds by adding the defined period to a given reference time.
 * </p>
 *
 * @param value        The quantity of the time unit (e.g., 30, 1, 6).
 * @param dateInterval The unit of time (e.g., DAYS, WEEKS, MONTHS).
 *
 * @author Sathish
 * @version 1.0
 */
public record Period(int value, DateInterval dateInterval) {

    /**
     * Calculates the UTC epoch milliseconds by adding this period to the
     * current system time.
     * * @return The calculated time in UTC milliseconds.
     */
    public long calculateUtcTime() {
        return calculateUtcTime(TemporalServiceProvider.get());
    }

    /**
     * Calculates the UTC epoch milliseconds by adding this period to a
     * specific {@link Instant}.
     * <p>
     * The input instant is converted to the UTC time zone before the period
     * is added to ensure consistent temporal arithmetic.
     * </p>
     * * @param inputTime The reference point in time.
     *
     * @return The resulting time in UTC milliseconds.
     */
    public long calculateUtcTime(final TemporalService temporalService) {
        return temporalService.plus(value, dateInterval);
    }

    /**
     * Returns a string representation of the period in the format: "{value} {chronoUnit}".
     * * @return A formatted string (e.g., "30 DAYS").
     */
    @Override
    public String toString() {
        return String.format("%s %s", value, dateInterval.toString());
    }
}
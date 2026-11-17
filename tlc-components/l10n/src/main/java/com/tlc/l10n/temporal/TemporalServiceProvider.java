package com.tlc.l10n.temporal;

import com.tlc.l10n.internal.temporal.TemporalServiceImpl;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.service.TemporalService;

import java.util.Calendar;

/**
 * <p>
 * Provides the object of {@link TemporalService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TemporalServiceProvider {

    /**
     * <p>
     * Provides the default {@link TemporalService}.
     * </p>
     *
     * @return object of {@link TemporalService}.
     */
    public static TemporalService get() {
        return new TemporalServiceImpl();
    }

    /**
     * <p>
     * Provides the {@link TemporalService} for the .
     * </p>
     *
     * @param date     input date value as string.
     * @param timezoneInfo {@link TimezoneInfo} refers to the regional or geographical area.
     * @return object of {@link TemporalService}.
     */
    public static TemporalService get(final String date, final TimezoneInfo timezoneInfo, final TemporalPatternType temporalPatternType) {
        return new TemporalServiceImpl(date, timezoneInfo, temporalPatternType);
    }

    /**
     * <p>
     * Sets the date long in {@link Calendar} and provides the {@link TemporalService}.
     * </p>
     *
     * @param date input date value in long.
     * @return object of {@link TemporalService}.
     */
    public static TemporalService get(final long date) {
        return new TemporalServiceImpl(date);
    }

    /**
     * <p>
     * Provides the {@link TemporalService} based on the given {@link TimezoneInfo}.
     * </p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo}.
     * @return {@link TemporalService}.
     */
    public static TemporalService get(final TimezoneInfo timezoneInfo) {
        return new TemporalServiceImpl(timezoneInfo);
    }
}

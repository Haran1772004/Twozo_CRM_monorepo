package com.tlc.crm.entity.service.filter;

import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.service.internal.DateIntervalServiceImpl;

import java.util.Map;

/**
 * <p>
 * Provides an abstract way to deal with {@link DateInterval}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DateIntervalService {

    /**
     * <p>
     * Gets the {@link DateIntervalService} instance.
     * </p>
     *
     * @return {@link DateIntervalService}.
     */
    static DateIntervalService getInstance() {
        return DateIntervalServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the date interval for the given id.
     * </p>
     *
     * @param dateIntervalId Represents the date interval id.
     * @return The {@link DateInterval}
     */
    DateInterval get(final Long dateIntervalId);

    /**
     * <p>
     * Fetches the {@link DateInterval} by using {@link DateIntervals}.
     * </p>
     *
     * @param dateIntervals Represents the date intervals.
     * @return {@link DateInterval} holds the details of the date interval.
     */
    DateInterval get(final DateIntervals dateIntervals);

    /**
     * <p>
     * Fetches all the available date intervals given by source id.
     * <p>
     *
     * @param includeDefaults A flag that indicates whether include the default date intervals or not.
     * @param sourceId        Represents the source id.
     * @return The outer {@link Map} date interval group, inner {@link Map} key as date interval id value as {@link DateInterval}.
     */
    Map<DateIntervalGroup, Map<Long, DateInterval>> getSourceIntervals(final boolean includeDefaults, final Long sourceId);
}
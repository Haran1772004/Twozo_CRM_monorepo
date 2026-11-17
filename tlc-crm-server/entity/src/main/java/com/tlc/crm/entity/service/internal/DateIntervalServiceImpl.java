package com.tlc.crm.entity.service.internal;

import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.crm.entity.resolver.filter.DateIntervalResolver;
import com.tlc.crm.entity.service.filter.DateIntervalService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implements the {@link DateIntervalService} provides functionality for accessing {@link DateInterval}.
 *
 * @author Kishore J
 * @version 1.0
 * @see DateIntervalService
 */
public class DateIntervalServiceImpl implements DateIntervalService {

    private static final Long DEFAULT_SOURCE_ID = -1L;

    private final DateIntervalResolver dateIntervalResolver;

    private DateIntervalServiceImpl() {
        dateIntervalResolver = DateIntervalResolver.getInstance();
    }

    private static class Instance {
        private static final DateIntervalServiceImpl INSTANCE = new DateIntervalServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link DateIntervalServiceImpl}.
     * </p>
     *
     * @return {@link DateIntervalServiceImpl}.
     */
    public static DateIntervalServiceImpl getInstance() {
        return Instance.INSTANCE;
    }


    /**
     * {@inheritDoc}
     *
     * @param dateIntervalId Represents the date interval id.
     * @return The {@link DateInterval}
     */
    @Override
    public DateInterval get(final Long dateIntervalId) {
        return dateIntervalResolver.getDateInterval(dateIntervalId);
    }

    /**
     * {@inheritDoc}
     *
     * @param dateIntervals Represents the date intervals.
     * @return {@link DateInterval} holds the details of the date interval.
     */
    @Override
    public DateInterval get(final DateIntervals dateIntervals) {
        return dateIntervalResolver.getAllDateIntervals().stream().filter(dateInterval -> Objects.equals(dateIntervals.name(),
                dateInterval.getName())).findAny().orElse(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param includeDefaults A flag that indicates whether include the default date intervals or not.
     * @param sourceId        Represents the source id.
     * @return The outer {@link Map} date interval group, inner {@link Map} key as date interval id value as {@link DateInterval}.
     */
    @Override
    public Map<DateIntervalGroup, Map<Long, DateInterval>> getSourceIntervals(final boolean includeDefaults, final Long sourceId) {
        final Map<DateIntervalGroup, Map<Long, DateInterval>> dateIntervals = new HashMap<>();
        final Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> groupedDateIntervals = dateIntervalResolver.getAll();

        if (includeDefaults) {
            dateIntervals.putAll(groupedDateIntervals.get(DEFAULT_SOURCE_ID));
        }

        if (Objects.nonNull(sourceId)) {
            final Map<DateIntervalGroup, Map<Long, DateInterval>> sourceDateIntervals = groupedDateIntervals.get(sourceId);

            if (Objects.nonNull(sourceDateIntervals)) {
                dateIntervals.putAll(sourceDateIntervals);
            }
        }

        return dateIntervals;
    }
}
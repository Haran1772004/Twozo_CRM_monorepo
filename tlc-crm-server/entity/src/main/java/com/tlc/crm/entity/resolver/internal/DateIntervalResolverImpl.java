package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.filter.DateIntervalCache;
import com.tlc.crm.entity.filter.api.manager.DateIntervalManager;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.resolver.filter.DateIntervalResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Resolves the {@link DateInterval} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see DateIntervalResolver
 */
public class DateIntervalResolverImpl implements DateIntervalResolver {

    private final DateIntervalCache dateIntervalCache;
    private final DateIntervalManager dateIntervalManager;

    private DateIntervalResolverImpl() {
        dateIntervalCache = DateIntervalCache.getInstance();
        dateIntervalManager = DateIntervalManager.getInstance();
    }

    private static class Instance {
        private static final DateIntervalResolverImpl INSTANCE = new DateIntervalResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link DateIntervalResolverImpl}.
     * </p>
     *
     * @return {@link DateIntervalResolverImpl}.
     */
    public static DateIntervalResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the date interval ID
     * @return The {@link DateInterval}
     */
    @Override
    public DateInterval getDateInterval(final Long id) {
        final DateInterval dateInterval = dateIntervalCache.get(id);

        if (Objects.isNull(dateInterval)) {
            final DateInterval sourceDateIntervals = dateIntervalManager.getDateInterval(id);

            dateIntervalCache.setDateInterval(sourceDateIntervals);

            return sourceDateIntervals;
        }

        return dateInterval;
    }

    /**
     * <p>
     * Fetches all the available {@link DateInterval}.
     * </p>
     *
     * @return Collection of {@link DateInterval} holds the details of the date interval.
     */
    public Collection<DateInterval> getAllDateIntervals() {
        final Collection<DateInterval> dateIntervals = dateIntervalCache.getAllDateIntervals();

        if (dateIntervals.isEmpty()) {
            final Collection<DateInterval> intervals = dateIntervalManager.getAllDateIntervals();

            for (final DateInterval dateInterval : intervals) {
                dateIntervalCache.setDateInterval(dateInterval);
            }
            return intervals;
        }

        return dateIntervals;
    }

    /**
     * {@inheritDoc}
     *
     * @return Grouped {@link DateInterval}.
     */
    @Override
    public Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> getAll() {
        final Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> dateIntervals = dateIntervalCache.getAll();

        if (Objects.isNull(dateIntervals) || dateIntervals.isEmpty()) {
            dateIntervalCache.setSourceDateInterval(dateIntervalManager.getAllDateIntervals());

            return dateIntervalCache.getAll();
        }

        return dateIntervals;
    }
}

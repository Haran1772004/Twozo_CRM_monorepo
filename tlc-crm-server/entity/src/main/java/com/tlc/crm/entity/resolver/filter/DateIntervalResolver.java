package com.tlc.crm.entity.resolver.filter;

import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;
import com.tlc.crm.entity.resolver.internal.DateIntervalResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A Resolver that resolves the {@link DateInterval} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface DateIntervalResolver {

    /**
     * <p>
     * Gets the instance of {@link DateIntervalResolver}.
     * </p>
     *
     * @return {@link DateIntervalResolver}.
     */
    static DateIntervalResolver getInstance() {
        return DateIntervalResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link DateInterval} for the provided ID. If not cached, it fetches data from the database,
     * caches it, and returns date interval from the cache.
     * </p>
     *
     * @param id Represents the date interval ID
     * @return The {@link DateInterval}
     */
    DateInterval getDateInterval(final Long id);

    /**
     * <p>
     * Fetches all the available {@link DateInterval}.
     * </p>
     *
     * @return Collection of {@link DateInterval} holds the details of the date interval.
     */
    Collection<DateInterval> getAllDateIntervals();

    /**
     * <p>
     * Groups all the date intervals by source id and group name.
     * </p>
     *
     * @return Grouped {@link DateInterval}.
     */
    Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> getAll();
}
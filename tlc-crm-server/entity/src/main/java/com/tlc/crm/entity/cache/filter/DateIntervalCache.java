package com.tlc.crm.entity.cache.filter;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.DateIntervalCacheImpl;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to deal with {@link DateInterval} objects.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface DateIntervalCache extends LocalCache {

    static DateIntervalCache getInstance() {
        return DateIntervalCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the date interval by id.
     * </p>
     *
     * @param id Date interval id.
     * @return Name of the date interval.
     */
    DateInterval get(final Long id);

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
     * Sets the date interval into the cache.
     * </p>
     *
     * @param dateInterval Represents the {@link DateInterval}
     */
    void setDateInterval(final DateInterval dateInterval);

    /**
     * <p>
     * Sets the date intervals details into the grouped source date interval cache.
     * </p>
     *
     * @param dateInterval Which holds the collection of {@link DateInterval}
     */
    void setSourceDateInterval(final Collection<DateInterval> dateInterval);

    /**
     * <p>
     * Gets all the date intervals from the cache.
     * </p>
     *
     * @return Grouped {@link DateInterval}.
     */
    Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> getAll();
}
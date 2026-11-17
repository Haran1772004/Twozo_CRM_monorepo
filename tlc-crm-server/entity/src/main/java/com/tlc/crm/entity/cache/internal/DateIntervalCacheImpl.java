package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.entity.cache.filter.DateIntervalCache;
import com.tlc.crm.entity.filter.api.manager.DateIntervalManager;
import com.tlc.crm.entity.filter.api.models.DateInterval;
import com.tlc.crm.entity.filter.api.models.DateIntervalGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Stores the date intervals into cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DateIntervalCacheImpl implements DateIntervalCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(DateIntervalCacheImpl.class);
    private static final Long DEFAULT_SOURCE_ID = -1L;

    private final Cache<Long, DateInterval> dateIntervalCache;
    private final Cache<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> groupedDateIntervalCache;
    private final DateIntervalManager valueManager;

    private static class Instance {
        private static final DateIntervalCacheImpl INSTANCE = new DateIntervalCacheImpl();
    }

    private DateIntervalCacheImpl() {
        final CacheManager cacheManager = CacheManager.getInstance();

        dateIntervalCache = cacheManager.createCache();
        groupedDateIntervalCache = cacheManager.createCache();
        valueManager = DateIntervalManager.getInstance();
    }

    /**
     * Gets the {@link DateIntervalCacheImpl} instance.
     *
     * @return {@link DateIntervalCacheImpl}.
     */
    public static DateIntervalCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<DateInterval> dateIntervals = valueManager.getAllDateIntervals();

        if (!dateIntervals.isEmpty()) {
            final Map<Long, DateInterval> dateIntervalMap = dateIntervals.stream().collect(Collectors.toMap(DateInterval::getId, dateInterval -> dateInterval));

            LOGGER.info("Total number of date interval entity loaded into cache : {}", dateIntervals.size());
            dateIntervalCache.putAll(dateIntervalMap);
            groupedDateIntervalCache.putAll(groupDateIntervals(dateIntervals));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        dateIntervalCache.clear();
        groupedDateIntervalCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Date interval id.
     * @return Name of the date interval.
     */
    @Override
    public DateInterval get(final Long id) {
        return dateIntervalCache.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link DateInterval} holds the details of the date interval.
     */
    @Override
    public Collection<DateInterval> getAllDateIntervals() {
        final Collection<DateInterval> dateIntervals = new ArrayList<>();

        dateIntervalCache.forEach((id, dateInterval) -> dateIntervals.add(dateInterval));
        return dateIntervals;
    }

    /**
     * <p>
     * Groups all the date intervals by source id and group name.
     * </p>
     *
     * @param dateIntervals Collection of {@link DateInterval}.
     * @return Grouped {@link DateInterval}.
     */
    private Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> groupDateIntervals(final Collection<DateInterval> dateIntervals) {
        final Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> groupedDateIntervals = new HashMap<>();

        for (final DateInterval dateInterval : dateIntervals) {
            final Long sourceId = dateInterval.getSourceId();
            final Map<DateIntervalGroup, Map<Long, DateInterval>> dateIntervalsGroup = groupedDateIntervals.computeIfAbsent(Objects.isNull(sourceId)
                    ? DEFAULT_SOURCE_ID : sourceId, map -> new HashMap<>());
            final Map<Long, DateInterval> dateIntervalMap = dateIntervalsGroup.computeIfAbsent(dateInterval.getGroup(), map -> new HashMap<>());

            dateIntervalMap.put(dateInterval.getId(), dateInterval);
        }

        return groupedDateIntervals;
    }

    /**
     * {@inheritDoc}
     *
     * @param dateInterval Represents the {@link DateInterval}
     */
    @Override
    public void setDateInterval(final DateInterval dateInterval) {
        dateIntervalCache.put(dateInterval.getId(), dateInterval);
    }

    /**
     * {@inheritDoc}
     *
     * @param dateIntervals Which holds the collection of {@link DateInterval}
     */
    @Override
    public void setSourceDateInterval(final Collection<DateInterval> dateIntervals) {
        groupedDateIntervalCache.putAll(groupDateIntervals(dateIntervals));
    }

    /**
     * <p>
     * Groups all the date intervals by source id and group name.
     * </p>
     *
     * @return Grouped {@link DateInterval}.
     */
    @Override
    public Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> getAll() {
        final Map<Long, Map<DateIntervalGroup, Map<Long, DateInterval>>> dateIntervalGroupMap = new HashMap<>();

        groupedDateIntervalCache.forEach(dateIntervalGroupMap::put);

        return dateIntervalGroupMap;
    }
}
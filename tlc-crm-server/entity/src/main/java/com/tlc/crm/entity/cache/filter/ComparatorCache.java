package com.tlc.crm.entity.cache.filter;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.entity.cache.internal.ComparatorCacheImpl;
import com.tlc.crm.entity.filter.api.models.Comparator;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with {@link Comparator}.
 *
 * @author Sathish
 * @version 1.0
 */
public interface ComparatorCache extends LocalCache {

    static ComparatorCache getInstance() {
        return ComparatorCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the comparators grouped by their id from the cache.
     * </p>
     *
     * @return The {@link Map} containing the key as comparator id value as {@link Comparator}.
     */
    Map<Long, Comparator> getComparators();

    /**
     * <p>
     * Fetches the type comparators grouped by their ids for the given field type id from the cache.
     * </p>
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} containing the key as comparator id value as {@link Comparator}.
     */
    Collection<Comparator> getComparatorsByType(final Long fieldTypeId);

    /**
     * <p>
     * Fetches the subtype comparators grouped by their ids for the given field subtype id from the cache.
     * </p>
     *
     * @param fieldSubTypeId Represents the field subtype id.
     * @return The {@link Map} containing the key as comparator id value as {@link Comparator}.
     */
    Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId);

    /**
     * <p>
     * Sets the comparators details into the cache.
     * </p>
     *
     * @param comparator Represents the {@link Comparator}
     */
    void addComparator(final Comparator comparator);
}

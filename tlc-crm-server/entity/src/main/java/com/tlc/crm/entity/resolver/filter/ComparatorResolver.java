package com.tlc.crm.entity.resolver.filter;

import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.resolver.internal.ComparatorResolverImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * A resolver that resolves the {@link Comparator} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface ComparatorResolver {

    /**
     * <p>
     * Gets the instance of {@link ComparatorResolver}.
     * </p>
     *
     * @return {@link ComparatorResolver}.
     */
    static ComparatorResolver getInstance() {
        return ComparatorResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all comparators grouped by their IDs.If not cached, fetches data from the database,
     * caches it, and returns comparators from the cache.
     * </p>
     *
     * @return The {@link Map} containing comparator ID and {@link Comparator}.
     */
    Map<Long, Comparator> getComparators();

    /**
     * <p>
     * Fetches comparators grouped by their IDs for the provided field type ID. If not cached,
     * fetches data from the database, caches it, and comparators returns from the cache.
     * </p>
     *
     * @param fieldTypeId Represents the field type ID.
     * @return The {@link Map} containing key as comparator ID value as {@link Comparator}.
     */
    Collection<Comparator> getComparatorsByType(final Long fieldTypeId);

    /**
     * <p>
     * Fetches comparators grouped by their IDs for the provided field subtype ID. If not cached,
     * fetches data from the database, caches it, and comparators returns from the cache.
     * </p>
     *
     * @param fieldSubTypeId Represents the field subtype ID.
     * @return The {@link Map} containing key as comparator ID value as {@link Comparator}.
     */
    Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId);
}
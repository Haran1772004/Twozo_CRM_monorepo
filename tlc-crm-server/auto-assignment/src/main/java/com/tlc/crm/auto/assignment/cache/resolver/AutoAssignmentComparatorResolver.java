package com.tlc.crm.auto.assignment.cache.resolver;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;

import java.util.Map;

/**
 * The resolver responsible for retrieving and storing details of the {@link AutoAssignmentComparatorResolver} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AutoAssignmentComparatorResolver {

    /**
     * Gets the {@link AutoAssignmentComparatorResolver} instance.
     *
     * @return Instance of {@link AutoAssignmentComparatorResolver}.
     */
    static AutoAssignmentComparatorResolver getInstance() {
        return AutoAssignmentComparatorResolverImpl.getInstance();
    }

    /**
     * Fetches all comparators grouped by their ids. If not present in cache, fetches data from DB and
     * stored it in cache and returns comparators from the cache.
     *
     * @return The {@link Map} of comparator and its identifier.
     */
    Map<Long, AutoAssignmentComparator> getAll();

    /**
     * Fetches all comparators grouped by their ids for the given fieldType. If not present in cache, fetches data from DB and
     * stored it in cache and returns comparators from the cache.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator and its identifier.
     */
    Map<Long, AutoAssignmentComparator> get(final Long fieldTypeId);
}

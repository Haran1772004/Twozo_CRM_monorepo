package com.tlc.crm.auto.assignment.cache;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.common.cache.LocalCache;

import java.util.Collection;
import java.util.Map;

/**
 * Stores the {@link AutoAssignmentComparatorCache} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AutoAssignmentComparatorCache extends LocalCache {

    /**
     * Gets the instance of {@link AutoAssignmentComparatorCache}.
     *
     * @return The {@link AutoAssignmentComparatorCache} instance.
     */
    static AutoAssignmentComparatorCache getInstance() {
        return AutoAssignmentComparatorCacheImpl.getInstance();
    }

    /**
     * Fetches the comparators by the given field type id.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator id and its details.
     */
    Map<Long, AutoAssignmentComparator> getComparators(final Long fieldTypeId);

    /**
     * Sets the comparators details into the cache.
     *
     * @param fieldTypeId          Represents field type id.
     * @param assignmentComparator Represents the {@link AutoAssignmentComparator}.
     */
    void setComparator(final Long fieldTypeId, final AutoAssignmentComparator assignmentComparator);

    /**
     * Sets the comparators details into the cache.
     *
     * @param comparators Represents the collection of {@link AutoAssignmentComparator} mapped with field type id.
     */
    void setComparators(final Map<Long, Collection<AutoAssignmentComparator>> comparators);

    /**
     * Fetches all comparators grouped by their id.
     *
     * @return The {@link Map} of comparator id and its details.
     */
    Map<Long, AutoAssignmentComparator> getAll();
}

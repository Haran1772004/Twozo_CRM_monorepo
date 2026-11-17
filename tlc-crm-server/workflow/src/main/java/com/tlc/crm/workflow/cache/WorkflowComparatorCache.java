package com.tlc.crm.workflow.cache;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.workflow.cache.internal.WorkflowComparatorCacheImpl;
import com.tlc.crm.workflow.api.model.WorkflowComparator;

import java.util.Collection;
import java.util.Map;

/**
 * Stores the {@link WorkflowComparator} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowComparatorCache extends LocalCache {

    /**
     * Gets the instance of {@link WorkflowComparatorCache}.
     *
     * @return The {@link WorkflowComparatorCache} instance.
     */
    static WorkflowComparatorCache getInstance() {
        return WorkflowComparatorCacheImpl.getInstance();
    }

    /**
     * Fetches the comparators by the given field type id.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator id and its details.
     */
    Map<Long, WorkflowComparator> getComparators(final Long fieldTypeId);

    /**
     * Sets the comparators details into the cache.
     *
     * @param fieldTypeId        Represents field type id.
     * @param workflowComparator Represents the {@link WorkflowComparator}.
     */
    void setComparator(final Long fieldTypeId, final WorkflowComparator workflowComparator);

    /**
     * Sets the comparators details into the cache.
     *
     * @param comparators Represents the collection of {@link WorkflowComparator} mapped with field type id.
     */
    void setComparators(final Map<Long, Collection<WorkflowComparator>> comparators);

    /**
     * Fetches all comparators grouped by their id.
     *
     * @return The {@link Map} of comparator id and its details.
     */
    Map<Long, WorkflowComparator> getAll();
}

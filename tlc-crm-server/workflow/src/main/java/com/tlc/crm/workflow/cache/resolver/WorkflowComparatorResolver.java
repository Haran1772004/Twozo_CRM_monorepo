package com.tlc.crm.workflow.cache.resolver;

import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.cache.resolver.internal.WorkflowComparatorResolverImpl;

import java.util.Map;

/**
 * The resolver responsible for retrieving and storing details of the {@link WorkflowComparator} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowComparatorResolver {

    /**
     * Gets the {@link WorkflowComparatorResolver} instance.
     *
     * @return Instance of {@link WorkflowComparatorResolver}.
     */
    static WorkflowComparatorResolver getInstance() {
        return WorkflowComparatorResolverImpl.getInstance();
    }

    /**
     * Fetches all comparators grouped by their ids. If not present in cache, fetches data from DB and
     * stored it in cache and returns comparators from the cache.
     *
     * @return The {@link Map} of comparator and its identifier.
     */
    Map<Long, WorkflowComparator> getAll();

    /**
     * Fetches all comparators grouped by their ids for the given fieldType. If not present in cache, fetches data from DB and
     * stored it in cache and returns comparators from the cache.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator and its identifier.
     */
    Map<Long, WorkflowComparator> get(final Long fieldTypeId);
}

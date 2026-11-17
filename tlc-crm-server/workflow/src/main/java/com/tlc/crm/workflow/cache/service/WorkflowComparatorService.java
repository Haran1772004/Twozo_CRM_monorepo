package com.tlc.crm.workflow.cache.service;

import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.cache.service.internal.WorkflowComparatorServiceImpl;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with {@link WorkflowComparator}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface WorkflowComparatorService {

    /**
     * Gets the {@link WorkflowComparatorService} instance.
     *
     * @return Instance of {@link WorkflowComparatorService}.
     */
    static WorkflowComparatorService getInstance() {
        return WorkflowComparatorServiceImpl.getInstance();
    }

    /**
     * Fetches the collection of {@link WorkflowComparator} given by field type id.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of {@link WorkflowComparator}.
     */
    Collection<WorkflowComparator> getComparatorsByFieldTypeId(final Long fieldTypeId);

    /**
     * Fetches the {@link WorkflowComparator} by id.
     *
     * @param id Represents the comparator id.
     * @return {@link WorkflowComparator}.
     */
    WorkflowComparator get(final Long id);

    /**
     * Fetches all the {@link WorkflowComparator}
     *
     * @return The {@link Map} of {@link WorkflowComparator} and its identifier.
     */
    Map<Long, WorkflowComparator> getAll();

    /**
     * Checks whether the comparators present or not for the given ids.
     *
     * @param comparatorIds Refers the collection of comparator ids.
     * @return true, If the comparators exist otherwise false.
     */
    boolean exists(final Collection<Long> comparatorIds);
}

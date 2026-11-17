package com.tlc.crm.auto.assignment.cache.service;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;

import java.util.Collection;
import java.util.Map;

/**
 * Provides an abstract way to deal with {@code AutoAssignmentComparator}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface AutoAssignmentComparatorService {

    /**
     * Gets the {@link AutoAssignmentComparatorService} instance.
     *
     * @return Instance of {@link AutoAssignmentComparatorService}.
     */
    static AutoAssignmentComparatorService getInstance() {
        return AutoAssignmentComparatorServiceImpl.getInstance();
    }

    /**
     * Fetches the collection of {@link AutoAssignmentComparator} given by field type id.
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of {@link AutoAssignmentComparator}.
     */
    Collection<AutoAssignmentComparator> getComparatorsByFieldTypeId(final Long fieldTypeId);

    /**
     * Fetches the {@link AutoAssignmentComparator} by id.
     *
     * @param id Represents the comparator id.
     * @return {@link AutoAssignmentComparator}.
     */
    AutoAssignmentComparator get(final Long id);

    /**
     * Fetches all the {@link AutoAssignmentComparator}
     *
     * @return The {@link Map} of {@link AutoAssignmentComparator} and its identifier.
     */
    Map<Long, AutoAssignmentComparator> getAll();

    /**
     * Checks whether the comparators present or not for the given ids.
     *
     * @param comparatorIds Refers the collection of comparator ids.
     * @return true, If the comparators exist otherwise false.
     */
    boolean exists(final Collection<Long> comparatorIds);

}

package com.tlc.crm.auto.assignment.cache.service;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.cache.resolver.AutoAssignmentComparatorResolver;
import com.tlc.crm.auto.assignment.internal.status.AutoAssignmentErrorCodes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages the service of {@code AutoAssignmentComparator}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class AutoAssignmentComparatorServiceImpl implements AutoAssignmentComparatorService {

    private final AutoAssignmentComparatorResolver assignmentComparatorResolver;

    private AutoAssignmentComparatorServiceImpl() {
        this.assignmentComparatorResolver = AutoAssignmentComparatorResolver.getInstance();
    }

    static class Instance {
        private static final AutoAssignmentComparatorServiceImpl COMPARATOR_SERVICE = new AutoAssignmentComparatorServiceImpl();
    }

    /**
     * Gets the {@link AutoAssignmentComparatorServiceImpl} instance.
     *
     * @return {@link AutoAssignmentComparatorServiceImpl}.
     */
    public static AutoAssignmentComparatorServiceImpl getInstance() {
        return Instance.COMPARATOR_SERVICE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of comparators.
     */
    @Override
    public Collection<AutoAssignmentComparator> getComparatorsByFieldTypeId(final Long fieldTypeId) {
        final Map<Long, AutoAssignmentComparator> comparators = assignmentComparatorResolver.get(fieldTypeId);

        if (Objects.isNull(comparators)) {
            throw ErrorCode.get(AutoAssignmentErrorCodes.COMPARATORS_NOT_FOUND, "i18n.auto.assignment.validation.invalid.comparator");
        }

        return List.copyOf(comparators.values());
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the comparator id.
     * @return The comparator.
     */
    @Override
    public AutoAssignmentComparator get(final Long id) {
        final AutoAssignmentComparator assignmentComparator = assignmentComparatorResolver.getAll().get(id);

        if (Objects.isNull(assignmentComparator)) {
            throw ErrorCode.get(AutoAssignmentErrorCodes.COMPARATORS_NOT_FOUND, "i18n.auto.assignment.validation.invalid.comparator");
        }

        return assignmentComparator;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} of {@link AutoAssignmentComparator} and its identifier.
     */
    @Override
    public Map<Long, AutoAssignmentComparator> getAll() {
        final Map<Long, AutoAssignmentComparator> assignmentComparators = assignmentComparatorResolver.getAll();

        if (Objects.isNull(assignmentComparators)) {
            throw ErrorCode.get(AutoAssignmentErrorCodes.COMPARATORS_NOT_FOUND, "i18n.auto.assignment.validation.invalid.comparator");
        }

        return assignmentComparators;
    }

    /**
     * {@inheritDoc}
     *
     * @param comparatorIds Refers the collection of comparator ids.
     * @return true, If the comparators exist otherwise false.
     */
    @Override
    public boolean exists(final Collection<Long> comparatorIds) {
        final Map<Long, AutoAssignmentComparator> comparators = assignmentComparatorResolver.getAll();

        for (final Long comparatorId : comparatorIds) {

            if (!comparators.containsKey(comparatorId)) {
                return false;
            }
        }

        return true;
    }

}

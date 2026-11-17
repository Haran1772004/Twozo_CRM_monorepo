package com.tlc.crm.workflow.cache.service.internal;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.cache.resolver.WorkflowComparatorResolver;
import com.tlc.crm.workflow.cache.service.WorkflowComparatorService;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link WorkflowComparatorService} designed to retrieve the comparator details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowComparatorServiceImpl implements WorkflowComparatorService {

    private final WorkflowComparatorResolver workflowComparatorResolver;

    private WorkflowComparatorServiceImpl() {
        this.workflowComparatorResolver = WorkflowComparatorResolver.getInstance();
    }

    private static class Instance {
        private static final WorkflowComparatorServiceImpl INSTANCE = new WorkflowComparatorServiceImpl();
    }

    /**
     * Gets the {@link WorkflowComparatorServiceImpl} instance.
     *
     * @return {@link WorkflowComparatorServiceImpl}.
     */
    public static WorkflowComparatorServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return The collection of comparators.
     */
    @Override
    public Collection<WorkflowComparator> getComparatorsByFieldTypeId(final Long fieldTypeId) {
        final Map<Long, WorkflowComparator> comparators = workflowComparatorResolver.get(fieldTypeId);

        if (Objects.isNull(comparators)) {
            throw ErrorCode.get(WorkflowErrorCodes.COMPARATORS_NOT_FOUND, "i18n.workflow.validation.invalid.comparator");
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
    public WorkflowComparator get(final Long id) {
        final WorkflowComparator workflowComparator = workflowComparatorResolver.getAll().get(id);

        if (Objects.isNull(workflowComparator)) {
            throw ErrorCode.get(WorkflowErrorCodes.COMPARATORS_NOT_FOUND, "i18n.workflow.validation.invalid.comparator");
        }

        return workflowComparator;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} of {@link WorkflowComparator} and its identifier.
     */
    @Override
    public Map<Long, WorkflowComparator> getAll() {
        final Map<Long, WorkflowComparator> workflowComparators = workflowComparatorResolver.getAll();

        if (Objects.isNull(workflowComparators)) {
            throw ErrorCode.get(WorkflowErrorCodes.COMPARATORS_NOT_FOUND, "i18n.workflow.validation.invalid.comparator");
        }

        return workflowComparators;
    }

    /**
     * {@inheritDoc}
     *
     * @param comparatorIds Refers the collection of comparator ids.
     * @return true, If the comparators exist otherwise false.
     */
    @Override
    public boolean exists(final Collection<Long> comparatorIds) {
        final Map<Long, WorkflowComparator> comparators = workflowComparatorResolver.getAll();

        for (final Long comparatorId : comparatorIds) {

            if (!comparators.containsKey(comparatorId)) {
                return false;
            }
        }

        return true;
    }
}

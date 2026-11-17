package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.manager.WorkflowFieldManager;
import com.tlc.crm.workflow.api.model.WorkflowField;
import com.tlc.crm.workflow.cache.WorkflowFieldCache;
import com.tlc.crm.workflow.cache.resolver.WorkflowFieldResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the implementation for the {@link WorkflowFieldResolver}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class WorkflowFieldResolverImpl implements WorkflowFieldResolver {

    private final WorkflowFieldCache workflowFieldCache;
    private final WorkflowFieldManager workflowFieldManager;
    private final ReentrantReadWriteLock lock;

    public WorkflowFieldResolverImpl() {
        this.workflowFieldCache = WorkflowFieldCache.getInstance();
        this.workflowFieldManager = WorkflowFieldManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final WorkflowFieldResolverImpl INSTANCE = new WorkflowFieldResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link WorkflowFieldResolverImpl}
     * </p>
     *
     * @return The {@link WorkflowFieldResolverImpl}
     */
    public static WorkflowFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param triggerActionSourceId Represents the trigger action source id
     * @return {@link Collection} of {@link WorkflowField}
     */
    @Override
    public Collection<WorkflowField> getWorkflowFieldsBySource(final Long triggerActionSourceId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Collection<WorkflowField> workflowFields;

        readLock.lock();

        try {
            workflowFields = workflowFieldCache.getWorkflowFieldsBySource(triggerActionSourceId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(workflowFields)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Collection<WorkflowField> fields = workflowFieldManager.getWorkflowFieldsBySource(triggerActionSourceId);

                workflowFieldCache.setWorkflowFields(fields);
                return fields;
            } finally {
                writeLock.unlock();
            }
        }
        return workflowFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the {@link WorkflowField} id
     * @return {@link WorkflowField}
     */
    @Override
    public WorkflowField get(final Long id) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final WorkflowField workflowField;

        readLock.lock();

        try {
            workflowField = workflowFieldCache.getWorkflowField(id);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(workflowField)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final WorkflowField field = workflowFieldManager.getWorkflowField(id);

                workflowFieldCache.setWorkflowField(field);
                return field;
            } finally {
                writeLock.unlock();
            }
        }
        return workflowField;
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Represents the {@link Collection} of workflow field id
     * @return true if all the ids are presented in workflow fields
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        if (ids.isEmpty()) {
            return false;
        } else {

            for (final Long id : ids) {

                if (Objects.isNull(get(id))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasNames {@link Collection} of alias names
     * @return {@link Map} of {@link WorkflowField} mapped for the given {@link Collection} of aliasNames
     */
    @Override
    public Map<String, WorkflowField> getWorkflowFields(final Collection<String> aliasNames) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final Map<String, WorkflowField> workflowFields;

        readLock.lock();

        try {
            workflowFields = workflowFieldCache.getWorkflowFields(aliasNames);
        } finally {
            readLock.unlock();
        }

        if (workflowFields.isEmpty()) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final Map<String, WorkflowField> fields = workflowFieldManager.getWorkflowFieldsByAliasNames(aliasNames);

                workflowFieldCache.setWorkflowFields(fields.values());

                return fields;
            } finally {
                writeLock.unlock();
            }
        }
        return workflowFields;
    }

    /**
     * {@inheritDoc}
     *
     * @param aliasName Represents the alias name
     * @return {@link WorkflowField}
     */
    @Override
    public WorkflowField getWorkflowField(final String aliasName) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final WorkflowField workflowField;

        readLock.lock();

        try {
            workflowField = workflowFieldCache.getWorkflowField(aliasName);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(workflowField)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final WorkflowField field = workflowFieldManager.getWorkflowFieldByAliasName(aliasName);

                workflowFieldCache.setWorkflowField(field);

                return field;
            } finally {
                writeLock.unlock();
            }
        }
        return workflowField;
    }
}

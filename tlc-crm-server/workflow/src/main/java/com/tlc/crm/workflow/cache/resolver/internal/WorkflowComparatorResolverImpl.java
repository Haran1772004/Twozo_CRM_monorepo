package com.tlc.crm.workflow.cache.resolver.internal;

import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.cache.WorkflowComparatorCache;
import com.tlc.crm.workflow.api.manager.WorkflowComparatorManager;
import com.tlc.crm.workflow.cache.resolver.WorkflowComparatorResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The resolver responsible for retrieving and storing details of the {@link WorkflowComparator} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowComparatorResolverImpl implements WorkflowComparatorResolver {

    private final WorkflowComparatorCache workflowComparatorCache;
    private final ReentrantReadWriteLock lock;
    private final WorkflowComparatorManager workflowComparatorManager;
    private final AtomicBoolean cacheLoaded;

    private WorkflowComparatorResolverImpl() {
        this.workflowComparatorCache = WorkflowComparatorCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.workflowComparatorManager = WorkflowComparatorManager.getInstance();
        this.cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final WorkflowComparatorResolverImpl INSTANCE = new WorkflowComparatorResolverImpl();
    }

    /**
     * Gets the {@link WorkflowComparatorResolverImpl} instance.
     *
     * @return {@link WorkflowComparatorResolverImpl}.
     */
    public static WorkflowComparatorResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} of comparator and its identifier.
     */
    @Override
    public Map<Long, WorkflowComparator> getAll() {
        if (!cacheLoaded.get()) {
            reloadComparator();
        }

        return workflowComparatorCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator and its identifier.
     */
    @Override
    public Map<Long, WorkflowComparator> get(final Long fieldTypeId) {
        final Map<Long, WorkflowComparator> comparators = workflowComparatorCache.getComparators(fieldTypeId);

        if (Objects.isNull(comparators)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                for (final WorkflowComparator workflowComparator : workflowComparatorManager.getComparatorsByFieldType(fieldTypeId)) {
                    workflowComparatorCache.setComparator(fieldTypeId, workflowComparator);
                }
                return workflowComparatorCache.getComparators(fieldTypeId);
            } finally {
                writeLock.unlock();
            }
        }

        return comparators;
    }

    /**
     * Reloads the comparators into cache.
     */
    private void reloadComparator() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Map<Long, Collection<WorkflowComparator>> comparators = workflowComparatorManager.getAll();

                workflowComparatorCache.setComparators(comparators);
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}

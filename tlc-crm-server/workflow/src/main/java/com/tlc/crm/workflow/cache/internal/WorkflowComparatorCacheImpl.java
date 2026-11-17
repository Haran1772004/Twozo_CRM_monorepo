package com.tlc.crm.workflow.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.workflow.api.manager.WorkflowComparatorManager;
import com.tlc.crm.workflow.api.model.WorkflowComparator;
import com.tlc.crm.workflow.cache.WorkflowComparatorCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores the {@link WorkflowComparator} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WorkflowComparatorCacheImpl implements WorkflowComparatorCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(WorkflowComparatorCacheImpl.class);

    private final Cache<Long, Map<Long, WorkflowComparator>> comparatorsCache;
    private final WorkflowComparatorManager workflowComparatorManager;
    private final ReentrantReadWriteLock lock;

    private WorkflowComparatorCacheImpl() {
        this.comparatorsCache = CacheManager.getInstance().createCache();
        this.workflowComparatorManager = WorkflowComparatorManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final WorkflowComparatorCacheImpl INSTANCE = new WorkflowComparatorCacheImpl();
    }

    /**
     * Gets the {@link WorkflowComparatorCacheImpl} instance.
     *
     * @return {@link WorkflowComparatorCacheImpl}.
     */
    public static WorkflowComparatorCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Map<Long, Collection<WorkflowComparator>> comparators = workflowComparatorManager.getAll();

        if (!comparators.isEmpty()) {
            final Map<Long, Map<Long, WorkflowComparator>> filterComparators = groupComparators(comparators);

            LOGGER.info("Total number of comparator data loaded into cache : {}", comparators.size());
            comparatorsCache.putAll(filterComparators);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator id and its details.
     */
    @Override
    public Map<Long, WorkflowComparator> getComparators(final Long fieldTypeId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            return comparatorsCache.get(fieldTypeId);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId        Represents field type id.
     * @param workflowComparator Represents the {@link WorkflowComparator}.
     */
    @Override
    public void setComparator(final Long fieldTypeId, final WorkflowComparator workflowComparator) {
        comparatorsCache.computeIfAbsent(fieldTypeId, comparatorMap -> new HashMap<>()).put(workflowComparator.getId(), workflowComparator);
    }

    /**
     * {@inheritDoc}
     *
     * @param comparators Represents the collection of {@link WorkflowComparator} mapped with field type id.
     */
    @Override
    public void setComparators(final Map<Long, Collection<WorkflowComparator>> comparators) {
        comparatorsCache.putAll(groupComparators(comparators));
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link WorkflowComparator}.
     */
    @Override
    public Map<Long, WorkflowComparator> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Map<Long, WorkflowComparator> fieldComparators = new HashMap<>();

            comparatorsCache.forEach((fieldTypeId, comparators) -> fieldComparators.putAll(comparators));

            return fieldComparators;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        comparatorsCache.clear();
    }

    /**
     * Groups the {@link WorkflowComparator} by field type.
     *
     * @param fieldTypeComparators Refers the list of comparators.
     * @return Comparators will be by grouped field type.
     */
    private Map<Long, Map<Long, WorkflowComparator>> groupComparators(final Map<Long, Collection<WorkflowComparator>> fieldTypeComparators) {
        final Map<Long, Map<Long, WorkflowComparator>> groupedFieldTypeComparators = new HashMap<>();

        for (final Map.Entry<Long, Collection<WorkflowComparator>> fieldTypeComparator : fieldTypeComparators.entrySet()) {
            final Map<Long, WorkflowComparator> comparators = groupedFieldTypeComparators.computeIfAbsent(fieldTypeComparator.getKey(), comparatorMap -> new HashMap<>());

            fieldTypeComparator.getValue().forEach(comparator -> comparators.put(comparator.getId(), comparator));
        }

        return groupedFieldTypeComparators;
    }
}

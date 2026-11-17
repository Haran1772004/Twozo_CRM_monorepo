package com.tlc.crm.auto.assignment.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentComparatorManager;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores the {@link AutoAssignmentComparatorCache} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class AutoAssignmentComparatorCacheImpl implements AutoAssignmentComparatorCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentComparatorCacheImpl.class);

    private final Cache<Long, Map<Long, AutoAssignmentComparator>> comparatorsCache;
    private final AutoAssignmentComparatorManager assignmentComparatorManager;
    private final ReentrantReadWriteLock lock;

    private AutoAssignmentComparatorCacheImpl() {
        this.comparatorsCache = CacheManager.getInstance().createCache();
        this.assignmentComparatorManager = AutoAssignmentComparatorManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    static class Instance {
        private static final AutoAssignmentComparatorCacheImpl COMPARATOR_CACHE = new AutoAssignmentComparatorCacheImpl();
    }

    /**
     * Gets the {@link AutoAssignmentComparatorCacheImpl} instance.
     *
     * @return {@link AutoAssignmentComparatorCacheImpl}.
     */
    public static AutoAssignmentComparatorCacheImpl getInstance() {
        return Instance.COMPARATOR_CACHE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Map<Long, Collection<AutoAssignmentComparator>> comparators = assignmentComparatorManager.getAll();

        if (!comparators.isEmpty()) {
            final Map<Long, Map<Long, AutoAssignmentComparator>> filterComparators = groupComparators(comparators);

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
    public Map<Long, AutoAssignmentComparator> getComparators(final Long fieldTypeId) {
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
     * @param fieldTypeId          Represents field type id.
     * @param assignmentComparator Represents the {@link AutoAssignmentComparator}.
     */
    @Override
    public void setComparator(final Long fieldTypeId, final AutoAssignmentComparator assignmentComparator) {
        comparatorsCache.computeIfAbsent(fieldTypeId, comparatorMap -> new HashMap<>()).put(assignmentComparator.getId(), assignmentComparator);
    }

    /**
     * {@inheritDoc}
     *
     * @param comparators Represents the collection of {@link AutoAssignmentComparator} mapped with field type id.
     */
    @Override
    public void setComparators(final Map<Long, Collection<AutoAssignmentComparator>> comparators) {
        comparatorsCache.putAll(groupComparators(comparators));
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link AutoAssignmentComparator}.
     */
    @Override
    public Map<Long, AutoAssignmentComparator> getAll() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Map<Long, AutoAssignmentComparator> fieldComparators = new HashMap<>();

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
     * Groups the {@link AutoAssignmentComparator} by field type.
     *
     * @param fieldTypeComparators Refers the list of comparators.
     * @return Comparators will be by grouped field type.
     */
    private Map<Long, Map<Long, AutoAssignmentComparator>> groupComparators(final Map<Long, Collection<AutoAssignmentComparator>> fieldTypeComparators) {
        final Map<Long, Map<Long, AutoAssignmentComparator>> groupedFieldTypeComparators = new HashMap<>();

        for (final Map.Entry<Long, Collection<AutoAssignmentComparator>> fieldTypeComparator : fieldTypeComparators.entrySet()) {
            final Map<Long, AutoAssignmentComparator> comparators = groupedFieldTypeComparators.computeIfAbsent(fieldTypeComparator.getKey(), comparatorMap -> new HashMap<>());

            fieldTypeComparator.getValue().forEach(comparator -> comparators.put(comparator.getId(), comparator));
        }

        return groupedFieldTypeComparators;
    }
}

package com.tlc.crm.auto.assignment.cache.resolver;

import com.tlc.crm.auto.assignment.api.manager.AutoAssignmentComparatorManager;
import com.tlc.crm.auto.assignment.api.model.AutoAssignmentComparator;
import com.tlc.crm.auto.assignment.cache.AutoAssignmentComparatorCache;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The resolver responsible for retrieving and storing details of the {@link AutoAssignmentComparatorResolver} from/to the cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class AutoAssignmentComparatorResolverImpl implements AutoAssignmentComparatorResolver {

    private final AutoAssignmentComparatorCache assignmentComparatorCache;
    private final ReentrantReadWriteLock lock;
    private final AutoAssignmentComparatorManager assignmentComparatorManager;
    private final AtomicBoolean cacheLoaded;

    private AutoAssignmentComparatorResolverImpl() {
        this.assignmentComparatorCache = AutoAssignmentComparatorCache.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.assignmentComparatorManager = AutoAssignmentComparatorManager.getInstance();
        this.cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final AutoAssignmentComparatorResolverImpl COMPARATOR_RESOLVER = new AutoAssignmentComparatorResolverImpl();
    }

    /**
     * Gets the {@link AutoAssignmentComparatorResolverImpl} instance.
     *
     * @return {@link AutoAssignmentComparatorResolverImpl}.
     */
    static AutoAssignmentComparatorResolverImpl getInstance() {
        return Instance.COMPARATOR_RESOLVER;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} of comparator and its identifier.
     */
    @Override
    public Map<Long, AutoAssignmentComparator> getAll() {
        if (!cacheLoaded.get()) {
            reloadComparator();
        }

        return assignmentComparatorCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type id.
     * @return The {@link Map} of comparator and its identifier.
     */
    @Override
    public Map<Long, AutoAssignmentComparator> get(final Long fieldTypeId) {
        final Map<Long, AutoAssignmentComparator> comparators = assignmentComparatorCache.getComparators(fieldTypeId);

        if (Objects.isNull(comparators)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                for (final AutoAssignmentComparator autoAssignmentComparator : assignmentComparatorManager.getComparatorsByFieldType(fieldTypeId)) {
                    assignmentComparatorCache.setComparator(fieldTypeId, autoAssignmentComparator);
                }
                return assignmentComparatorCache.getComparators(fieldTypeId);
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
                final Map<Long, Collection<AutoAssignmentComparator>> comparators = assignmentComparatorManager.getAll();

                assignmentComparatorCache.setComparators(comparators);
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }

}

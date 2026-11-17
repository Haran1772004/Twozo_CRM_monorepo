package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.filter.ComparatorCache;
import com.tlc.crm.entity.filter.api.manager.ComparatorManager;
import com.tlc.crm.entity.filter.api.models.Comparator;
import com.tlc.crm.entity.resolver.filter.ComparatorResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the {@link Comparator} related details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 * @see ComparatorResolver
 */
public class ComparatorResolverImpl implements ComparatorResolver {

    private final ComparatorCache comparatorCache;
    private final ReentrantReadWriteLock lock;
    private final ComparatorManager comparatorManager;
    private final AtomicBoolean cacheLoaded;

    private ComparatorResolverImpl() {
        comparatorCache = ComparatorCache.getInstance();
        lock = new ReentrantReadWriteLock();
        comparatorManager = ComparatorManager.getInstance();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final ComparatorResolverImpl INSTANCE = new ComparatorResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link ComparatorResolverImpl}.
     * </p>
     *
     * @return {@link ComparatorResolverImpl}.
     */
    public static ComparatorResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing comparator ID and {@link Comparator}.
     */
    @Override
    public Map<Long, Comparator> getComparators() {
        if (!cacheLoaded.get()) {
            reloadComparator();
        }

        return comparatorCache.getComparators();
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the field type ID.
     * @return The {@link Map} containing the comparator id and {@link Comparator}.
     */
    @Override
    public Collection<Comparator> getComparatorsByType(final Long fieldTypeId) {
        final Collection<Comparator> comparators = comparatorCache.getComparatorsByType(fieldTypeId);

        if (Objects.isNull(comparators) || comparators.isEmpty()) {
            for (final Comparator comparator : comparatorManager.getComparatorsByType(fieldTypeId)) {
                comparatorCache.addComparator(comparator);
            }
            return comparatorCache.getComparatorsByType(fieldTypeId);
        }

        return comparators;
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSubTypeId Represents the field subtype ID.
     * @return The {@link Map} containing key as comparator ID value as {@link Comparator}.
     */
    @Override
    public Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId) {
        final Collection<Comparator> comparators = comparatorCache.getComparatorsBySubType(fieldSubTypeId);

        if (Objects.isNull(comparators) || comparators.isEmpty()) {
            for (final Comparator comparator : comparatorManager.getComparatorsBySubType(fieldSubTypeId)) {
                comparatorCache.addComparator(comparator);
            }
            return comparatorCache.getComparatorsBySubType(fieldSubTypeId);
        }

        return comparators;
    }

    /**
     * <p>
     * Reloads the comparators into cache.
     * </p>
     */
    private void reloadComparator() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                for (final Comparator comparator : comparatorManager.getAll()) {
                    comparatorCache.addComparator(comparator);
                }
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
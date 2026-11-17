package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.entity.cache.filter.ComparatorCache;
import com.tlc.crm.entity.filter.api.manager.ComparatorManager;
import com.tlc.crm.entity.filter.api.models.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Stores the comparators into cache.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ComparatorCacheImpl implements ComparatorCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(ComparatorCacheImpl.class);

    private final Cache<Long, Comparator> comparatorCache;
    private final Cache<Comparator.Specialization, Map<Long, Collection<Comparator>>> specializedComparatorCache;
    private final ComparatorManager comparatorManager;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final ComparatorCacheImpl INSTANCE = new ComparatorCacheImpl();
    }

    private ComparatorCacheImpl() {
        final CacheManager cacheManager = CacheManager.getInstance();

        comparatorCache = cacheManager.createCache();
        specializedComparatorCache = cacheManager.createCache();
        comparatorManager = ComparatorManager.getInstance();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Gets the {@link ComparatorCacheImpl} instance.
     *
     * @return {@link ComparatorCacheImpl}.
     */
    public static ComparatorCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<Comparator> comparators = comparatorManager.getAll();

        if (!comparators.isEmpty()) {
            final Map<Long, Comparator> groupedComparators = new HashMap<>();
            final Map<Comparator.Specialization, Map<Long, Collection<Comparator>>> specializedComparatorGroup = new HashMap<>();

            for (final Comparator comparator : comparators) {
                final Comparator.Specialization specialization = comparator.getSpecialization();

                if (Objects.nonNull(specialization)) {
                    final Map<Long, Collection<Comparator>> specializedComparators = specializedComparatorGroup
                            .computeIfAbsent(specialization, comparatorSpecialization -> new HashMap<>());

                    specializedComparators.computeIfAbsent(comparator.getSpecializationId(), specializationId -> new HashSet<>()).add(comparator);
                }
                groupedComparators.put(comparator.getId(), comparator);
            }
            LOGGER.info("Total number of comparator entity loaded into cache : {}", comparators.size());
            comparatorCache.putAll(groupedComparators);
            specializedComparatorCache.putAll(specializedComparatorGroup);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        specializedComparatorCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing the key as comparator id value as comparator object.
     */
    @Override
    public Map<Long, Comparator> getComparators() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Map<Long, Comparator> comparators = new HashMap<>();

            comparatorCache.forEach(comparators::put);

            return comparators;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldTypeId Represents the fieldType id.
     * @return The {@link Map} containing the key as comparator id value as comparator object.
     */
    @Override
    public Collection<Comparator> getComparatorsByType(final Long fieldTypeId) {
        final Map<Long, Collection<Comparator>> typeComparators = specializedComparatorCache.get(Comparator.Specialization.TYPE);

        return Objects.isNull(typeComparators) ? Collections.emptyList() : typeComparators.get(fieldTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param fieldSubTypeId Represents the field subtype id.
     * @return The {@link Map} containing the key as comparator id value as {@link Comparator}.
     */
    @Override
    public Collection<Comparator> getComparatorsBySubType(final Long fieldSubTypeId) {
        final Map<Long, Collection<Comparator>> typeComparators = specializedComparatorCache.get(Comparator.Specialization.SUB_TYPE);

        return Objects.isNull(typeComparators) ? Collections.emptyList() : typeComparators.get(fieldSubTypeId);
    }

    /**
     * {@inheritDoc}
     *
     * @param comparator Represents the {@link Comparator}
     */
    @Override
    public void addComparator(final Comparator comparator) {
        final Comparator.Specialization specialization = comparator.getSpecialization();

        if (Objects.nonNull(specialization)) {
            final Map<Long, Collection<Comparator>> specializedComparators = specializedComparatorCache
                    .computeIfAbsent(comparator.getSpecialization(), comparatorSpecialization -> new HashMap<>());

            specializedComparators.computeIfAbsent(comparator.getSpecializationId(), specializationId -> new HashSet<>()).add(comparator);
        }
        comparatorCache.put(comparator.getId(), comparator);
    }
}

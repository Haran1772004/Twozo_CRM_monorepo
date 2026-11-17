package com.tlc.crm.entity.resolver.internal;

import com.tlc.crm.entity.cache.filter.FilterSourceCache;
import com.tlc.crm.entity.resolver.filter.FilterSourceResolver;
import com.tlc.crm.fields.manager.FieldSourceManager;
import com.tlc.crm.fields.models.FieldSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link FieldSource} related details.
 *
 * @author Kishore J
 * @version 1.0
 * @see FilterSourceResolver
 */
public class FilterSourceResolverImpl implements FilterSourceResolver {

    private final FilterSourceCache filterSourceCache;
    private final FieldSourceManager fieldSourceManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private FilterSourceResolverImpl() {
        filterSourceCache = FilterSourceCache.getInstance();
        fieldSourceManager = FieldSourceManager.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final FilterSourceResolverImpl INSTANCE = new FilterSourceResolverImpl();
    }

    /**
     * Gets the instance of {@link FilterSourceResolverImpl}.
     *
     * @return {@link FilterSourceResolverImpl}.
     */
    public static FilterSourceResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the source ID
     * @return {@link FieldSource} that holds the details of the source.
     */
    @Override
    public FieldSource get(final Long id) {
        final FieldSource fieldSource = filterSourceCache.get(id);

        if (Objects.isNull(fieldSource)) {
            final FieldSource sourceField = fieldSourceManager.getFilterSource(id);

            if (Objects.nonNull(sourceField)) {
                filterSourceCache.setFilterSource(sourceField);
            }

            return sourceField;
        }

        return fieldSource;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of {@link FieldSource}.
     */
    @Override
    public Collection<FieldSource> getAll() {
        if (!cacheLoaded.get()) {
            reloadFilterSource();
        }

        return filterSourceCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param ids Collection of field source IDs.
     * @return A flag that indicates all the given IDs are present or not in the {@link FieldSource}
     */
    @Override
    public boolean exists(final Collection<Long> ids) {
        final Collection<Long> sourceIds = new HashSet<>();

        for (final Long id : ids) {

            if (Objects.nonNull(get(id))) {
                sourceIds.add(id);
            }
        }

        return sourceIds.containsAll(ids);
    }

    /**
     * <p>
     * Reloads the filter source into cache.
     * </p>
     */
    private void reloadFilterSource() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<FieldSource> filterSources = fieldSourceManager.getFilterSources();

                if (Objects.nonNull(filterSources)) {
                    for (final FieldSource filterSource : filterSources) {
                        filterSourceCache.setFilterSource(filterSource);
                    }
                }
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
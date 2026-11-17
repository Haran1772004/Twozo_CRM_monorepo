package com.tlc.crm.fields.resolver.internal;

import com.tlc.crm.fields.cache.FieldSourceCache;
import com.tlc.crm.fields.manager.FieldSourceManager;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.resolver.FieldSourceResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link FieldSource} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldSourceResolver
 */
public class FieldSourceResolverImpl implements FieldSourceResolver {

    private final FieldSourceCache fieldSourceCache;
    private final FieldSourceManager fieldSourceManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private FieldSourceResolverImpl() {
        fieldSourceCache = FieldSourceCache.getInstance();
        fieldSourceManager = FieldSourceManager.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final FieldSourceResolverImpl INSTANCE = new FieldSourceResolverImpl();
    }

    /**
     * Gets the instance of {@link FieldSourceResolverImpl}.
     *
     * @return {@link FieldSourceResolverImpl}.
     */
    public static FieldSourceResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of field source entities
     */
    @Override
    public Collection<FieldSource> getAll() {
        if (!cacheLoaded.get()) {
            reloadSource();
        }

        return fieldSourceCache.getSources();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source ID
     * @return The {@link FieldSource}
     */
    @Override
    public FieldSource get(final Long sourceId) {
        final FieldSource source = fieldSourceCache.getSource(sourceId);

        if (Objects.isNull(source)) {
            final FieldSource fieldSource = fieldSourceManager.get(sourceId);

            if (Objects.nonNull(fieldSource)) {
                fieldSourceCache.setSourceCache(fieldSource);
            }

            return fieldSource;
        }

        return source;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as field source ID value as {@link FieldSource}.
     */
    @Override
    public Map<Long, FieldSource> groupSources() {
        if (!cacheLoaded.get()) {
            reloadSource();
        }

        return fieldSourceCache.getAllFieldSources();
    }

    /**
     * <p>
     * Reloads the field source into cache.
     * </p>
     */
    private void reloadSource() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<FieldSource> fieldSources = fieldSourceManager.getAllEntitySources();

                if (Objects.nonNull(fieldSources) && !fieldSources.isEmpty()) {

                    for (final FieldSource fieldSource : fieldSources) {
                        fieldSourceCache.setSourceCache(fieldSource);
                    }
                    cacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}
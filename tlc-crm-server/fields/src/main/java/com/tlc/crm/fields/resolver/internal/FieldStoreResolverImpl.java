package com.tlc.crm.fields.resolver.internal;

import com.tlc.crm.fields.cache.FieldStoreCache;
import com.tlc.crm.fields.manager.FieldStoreManager;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.resolver.FieldStoreResolver;
import com.tlc.crm.fields.service.FieldSourceService;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link FieldStore} related details
 *
 * @author Kishore J
 * @version 1.0
 * @see FieldStoreResolver
 */
public class FieldStoreResolverImpl implements FieldStoreResolver {

    private final FieldStoreManager fieldStoreManager;
    private final FieldStoreCache fieldStoreCache;
    private final FieldSourceService fieldSourceService;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private FieldStoreResolverImpl() {
        fieldStoreCache = FieldStoreCache.getInstance();
        fieldStoreManager = FieldStoreManager.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final FieldStoreResolverImpl INSTANCE = new FieldStoreResolverImpl();
    }

    /**
     * Gets the instance of {@link FieldStoreResolverImpl}.
     *
     * @return {@link FieldStoreResolverImpl}.
     */
    public static FieldStoreResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the store ID
     * @return {@link FieldStore}
     */
    @Override
    public FieldStore get(final Long id) {
        if (!cacheLoaded.get()) {
            reloadFieldStore();
        }

        return fieldStoreCache.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link FieldStore}
     */
    @Override
    public Collection<FieldStore> getAll() {
        if (!cacheLoaded.get()) {
            reloadFieldStore();
        }

        return fieldStoreCache.getFieldStores();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents the source name.
     * @return Collection of {@link FieldStore}
     */
    @Override
    public Collection<FieldStore> getFieldStoresBySource(final String sourceName) {
        final Map<Long, FieldStore> fieldStores = fieldStoreCache.getFieldStoresBySource(sourceName);

        if (Objects.isNull(fieldStores)) {
            final FieldSource fieldSource = fieldSourceService.getSource(sourceName);
            final Collection<FieldStore> sourceFieldStores = fieldStoreManager.getFieldStoresBySourceId(fieldSource.id());

            if (Objects.nonNull(sourceFieldStores) && !sourceFieldStores.isEmpty()) {

                for (final FieldStore fieldStore : sourceFieldStores) {
                    fieldStoreCache.setFieldStore(sourceName, fieldStore);
                }

                return sourceFieldStores;
            }
        }

        return fieldStores.values();
    }

    /**
     * <p>
     * Reloads all the field stores into cache
     * </p>
     */
    private void reloadFieldStore() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                final Collection<FieldStore> fieldStores = fieldStoreManager.getAllFieldStore();
                final Map<Long, FieldSource> sources = fieldSourceService.groupSources();

                if (Objects.nonNull(fieldStores) && !fieldStores.isEmpty()) {

                    for (final FieldStore fieldStore : fieldStores) {
                        fieldStoreCache.setFieldStore(sources.get(fieldStore.sourceId()).getName(), fieldStore);
                    }
                    cacheLoaded.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}

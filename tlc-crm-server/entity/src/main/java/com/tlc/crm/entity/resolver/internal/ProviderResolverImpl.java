package com.tlc.crm.entity.resolver.internal;

import com.tlc.commons.pair.Pair;
import com.tlc.crm.entity.cache.list.ProviderCache;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.list.ProviderManager;
import com.tlc.crm.entity.resolver.list.ProviderResolver;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Resolves the {@link Provider} related details
 *
 * @author kishore J
 * @version 1.0
 * @see ProviderResolver
 */
public class ProviderResolverImpl implements ProviderResolver {

    private final ProviderCache providerCache;
    private final ProviderManager providerManager;
    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean cacheLoaded;

    private ProviderResolverImpl() {
        providerCache = ProviderCache.getInstance();
        providerManager = ProviderManager.getInstance();
        lock = new ReentrantReadWriteLock();
        cacheLoaded = new AtomicBoolean();
    }

    private static class Instance {
        private static final ProviderResolverImpl INSTANCE = new ProviderResolverImpl();
    }

    /**
     * <p>
     * Gets the instance of  {@link ProviderResolverImpl}
     * </p>
     *
     * @return {@link ProviderResolverImpl}
     */
    public static ProviderResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as provider ID value as {@link Provider}
     */
    @Override
    public Map<Long, Provider> getAll() {
        if (!cacheLoaded.get()) {
            reloadProvider();
        }

        return providerCache.getAll();
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source ID.
     * @return The {@link Map.Entry} contains the key as provider ID value as {@link Provider}
     */
    @Override
    public Pair<Long, Provider> getProvider(final Long sourceId) {
        final Pair<Long, Provider> providerEntry = providerCache.get(sourceId);

        if (Objects.isNull(providerEntry)) {
            final Pair<Long, Provider> providerPair = providerManager.fetchProvider(sourceId);

            providerCache.setProvider(sourceId, providerPair);

            return providerPair;
        }

        return providerEntry;
    }

    /**
     * <p>
     * Reloads the providers into cache.
     * </p>
     */
    private void reloadProvider() {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (!cacheLoaded.get()) {
                for (final Map.Entry<Long, Pair<Long, Provider>> providersEntry : providerManager.fetchAll().entrySet()) {
                    providerCache.setProvider(providersEntry.getKey(), providersEntry.getValue());
                }
                cacheLoaded.set(true);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
package com.tlc.crm.zapier.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;

import com.tlc.crm.zapier.api.manager.ZapierFieldManager;
import com.tlc.crm.zapier.api.models.ZapierField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides the way to stores the {@link ZapierField} into cache.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
final class ZapierFieldCacheImpl implements ZapierFieldCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(ZapierFieldCacheImpl.class);

    private final Cache<Short, Collection<ZapierField>> zapierFieldCache;
    private final ZapierFieldManager zapierFieldManager;
    private final ReentrantReadWriteLock lock;

    private ZapierFieldCacheImpl() {
        final CacheConfig<Short, Collection<ZapierField>> cacheConfig = new CacheConfig<>();

        cacheConfig.setExpireAfterAccess(TimeUnit.MINUTES.toMillis(30));
        this.zapierFieldCache = CacheManager.getInstance().createCache(cacheConfig);
        this.zapierFieldManager = ZapierFieldManager.getInstance();
        this.lock = new ReentrantReadWriteLock();
    }

    private static class Instance {
        private static final ZapierFieldCacheImpl INSTANCE = new ZapierFieldCacheImpl();
    }

    /**
     * Retrieves the {@link ZapierFieldCacheImpl} instance.
     *
     * @return The {@link ZapierFieldCacheImpl} instance.
     */
    public static ZapierFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Map<Short, Collection<ZapierField>> zapierFields = zapierFieldManager.getAll();

        if (!zapierFields.isEmpty()) {

            LOGGER.info("Zapier: Total number of zapier fields data loaded into cache: {}", zapierFields.size());
            zapierFieldCache.putAll(zapierFields);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        zapierFieldCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @param zapActionId The action ID for which the fields are to be retrieved.
     */
    @Override
    public Collection<ZapierField> getFieldsByAction(final Short zapActionId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            return zapierFieldCache.get(zapActionId);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id     The action ID to which the fields are associated.
     * @param fields A collection of ZapierField objects to set or update for the given action ID.
     */
    @Override
    public void setFields(final Short id, final Collection<ZapierField> fields) {
        zapierFieldCache.put(id, fields);
    }
}

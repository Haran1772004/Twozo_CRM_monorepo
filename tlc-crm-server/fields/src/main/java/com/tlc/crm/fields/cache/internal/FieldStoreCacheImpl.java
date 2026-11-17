package com.tlc.crm.fields.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.fields.cache.FieldStoreCache;
import com.tlc.crm.fields.internal.status.FieldErrorCodes;
import com.tlc.crm.fields.manager.FieldStoreManager;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.crm.fields.models.FieldStore;
import com.tlc.crm.fields.service.FieldSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Stores the field store details into cache and provides API to access the it
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public class FieldStoreCacheImpl implements FieldStoreCache {

    private final Logger LOGGER = LoggerFactory.getLogger(FieldStoreCacheImpl.class);

    private final Cache<String, Map<Long, FieldStore>> FIELDS_STORE_CACHE;
    private final FieldSourceService fieldSourceService;
    private final FieldStoreManager fieldStoreManager;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final FieldStoreCacheImpl INSTANCE = new FieldStoreCacheImpl();
    }

    public static FieldStoreCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    private FieldStoreCacheImpl() {
        FIELDS_STORE_CACHE = CacheManager.getInstance().createCache();
        fieldStoreManager = FieldStoreManager.getInstance();
        fieldSourceService = FieldSourceService.getInstance();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Collection<FieldStore> fieldStores = fieldStoreManager.getAllFieldStore();
        final Map<Long, FieldSource> sources = fieldSourceService.groupSources();

        if (Objects.nonNull(fieldStores) && !fieldStores.isEmpty()) {

            for (FieldStore fieldStore : fieldStores) {
                final FieldSource fieldSource = sources.get(fieldStore.sourceId());
                final Map<Long, FieldStore> sourceFields = FIELDS_STORE_CACHE.computeIfAbsent(fieldSource.getName(), source -> new HashMap<>());

                sourceFields.put(fieldStore.id(), fieldStore);
            }

            LOGGER.info("Field stores successfully loaded into cache");
        } else {
            throw ErrorCode.get(FieldErrorCodes.FAILED_LOAD_CRM_FIELD_STORES_INTO_CACHE);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        FIELDS_STORE_CACHE.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @return The collection of {@link FieldStore}
     */
    @Override
    public Collection<FieldStore> getFieldStores() {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            final Collection<FieldStore> fieldStores = new ArrayList<>();

            FIELDS_STORE_CACHE.forEach((key, crmFieldStore) -> fieldStores.addAll(crmFieldStore.values()));

            return fieldStores;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param id Represents the field store id
     * @return {@link FieldStore}
     */
    @Override
    public FieldStore get(final Long id) {
        final List<FieldStore> stores = new ArrayList<>(1);

        FIELDS_STORE_CACHE.forEach((sourceName, fieldStoreMap) -> {
            final FieldStore fieldStore = fieldStoreMap.get(id);

            if (!Objects.isNull(fieldStore)) {
                stores.add(fieldStore);
            }
        });

        return stores.get(0);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents the source name
     * @param fieldStore Represents the {@link FieldStore}
     */
    @Override
    public void setFieldStore(final String sourceName, final FieldStore fieldStore) {
        FIELDS_STORE_CACHE.computeIfAbsent(sourceName, map -> new HashMap<>()).put(fieldStore.id(), fieldStore);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceName Represents the source name
     * @return The {@link Map} containing key as field store id value as {@link FieldStore}.
     */
    @Override
    public Map<Long, FieldStore> getFieldStoresBySource(final String sourceName) {
        return FIELDS_STORE_CACHE.get(sourceName);
    }
}

package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.function.ConfigurableFunctionDetailCache;
import com.tlc.analytics.config.manager.function.ConfigurableFunctionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link ConfigurableFunctionDetailCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ConfigurableFunctionDetailCacheImpl implements ConfigurableFunctionDetailCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableFunctionDetailCacheImpl.class);
    private final Cache<Long, Collection<ConfigurableFunctionDetail>> cacheByColumnId;
    private final Cache<Long, ConfigurableFunctionDetail> cacheById;
    private final ConfigurableFunctionManager configurableFunctionManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private ConfigurableFunctionDetailCacheImpl() {
        this.cacheByColumnId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.configurableFunctionManager = ConfigurableFunctionManager.INSTANCE();
    }

    private static class Instance {
        private static final ConfigurableFunctionDetailCacheImpl INSTANCE = new ConfigurableFunctionDetailCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link ConfigurableFunctionDetailCacheImpl} instance.
     * </p>
     *
     * @return {@link ConfigurableFunctionDetailCacheImpl}
     */
    public static ConfigurableFunctionDetailCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        configurableFunctionManager.fetchAll().forEach(functionDetail -> {
            cacheById.put(functionDetail.id(), functionDetail);

            final Long columnId = functionDetail.getColumnId();
            Collection<ConfigurableFunctionDetail> existing = cacheByColumnId.get(columnId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByColumnId.put(columnId, existing);
            }

            existing.add(functionDetail);
        });

        LOGGER.info("CRM Analytics : ConfigurableFunctionDetail loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByColumnId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : ConfigurableFunctionDetail cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ConfigurableFunctionDetail> get(final Long columnId) {
        return cacheByColumnId.get(columnId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurableFunctionDetail getById(final Long functionId) {
        return cacheById.get(functionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Long columnId, final Collection<ConfigurableFunctionDetail> functionDetails) {
        cacheByColumnId.put(columnId, functionDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long functionId, final ConfigurableFunctionDetail functionDetail) {
        cacheById.put(functionId, functionDetail);
    }

}

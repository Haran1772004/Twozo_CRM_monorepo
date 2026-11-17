package com.tlc.analytics.cache.internal;

import com.tlc.analytics.config.function.ParentConfigurableFunctionDetail;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.analytics.cache.function.ParentConfigurableFunctionDetailCache;
import com.tlc.analytics.config.manager.function.ConfigurableFunctionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link ParentConfigurableFunctionDetailCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class ParentConfigurableFunctionDetailCacheImpl implements ParentConfigurableFunctionDetailCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParentConfigurableFunctionDetailCacheImpl.class);
    private final Cache<Long, Collection<ParentConfigurableFunctionDetail>> cacheByColumnFunctionId;
    private final Cache<Long, ParentConfigurableFunctionDetail> cacheById;
    private final ConfigurableFunctionManager configurableFunctionManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private ParentConfigurableFunctionDetailCacheImpl() {
        this.cacheByColumnFunctionId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.configurableFunctionManager = ConfigurableFunctionManager.INSTANCE();
    }

    private static class Instance {
        private static final ParentConfigurableFunctionDetailCacheImpl INSTANCE = new ParentConfigurableFunctionDetailCacheImpl();
    }

    /**
     * <p>
     * Retrieves the {@link ParentConfigurableFunctionDetailCacheImpl} instance.
     * </p>
     *
     * @return {@link ParentConfigurableFunctionDetailCacheImpl}
     */
    public static ParentConfigurableFunctionDetailCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        configurableFunctionManager.fetchAllParent().forEach(parentFunctionDetail -> {
            cacheById.put(parentFunctionDetail.id(), parentFunctionDetail);

            final Long columnFunctionId = parentFunctionDetail.getColumnFunctionId();
            Collection<ParentConfigurableFunctionDetail> existing = cacheByColumnFunctionId.get(columnFunctionId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByColumnFunctionId.put(columnFunctionId, existing);
            }

            existing.add(parentFunctionDetail);
        });

        LOGGER.info("CRM Analytics : ParentConfigurableFunctionDetail loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByColumnFunctionId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : ParentConfigurableFunctionDetail cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ParentConfigurableFunctionDetail> get(final Long columnFunctionId) {
        return cacheByColumnFunctionId.get(columnFunctionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParentConfigurableFunctionDetail getById(final Long parentFunctionId) {
        return cacheById.get(parentFunctionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Long columnFunctionId, final Collection<ParentConfigurableFunctionDetail> parentFunctionDetails) {
        cacheByColumnFunctionId.put(columnFunctionId, parentFunctionDetails);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long parentFunctionId, final ParentConfigurableFunctionDetail parentFunctionDetail) {
        cacheById.put(parentFunctionId, parentFunctionDetail);
    }

}

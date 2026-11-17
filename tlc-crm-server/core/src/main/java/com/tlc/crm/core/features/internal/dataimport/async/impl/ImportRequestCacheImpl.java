package com.tlc.crm.core.features.internal.dataimport.async.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.cache.listener.ExpiryListener;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequest;
import com.tlc.crm.core.features.internal.dataimport.async.req.ImportRequestCache;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cache to store the {@link ImportRequest}
 *
 * @author sundar
 * @version 1.0
 */
public class ImportRequestCacheImpl implements ImportRequestCache {

    private final Cache<String, ImportRequest> importRequestCache;

    private ImportRequestCacheImpl() {
        final CacheConfig<String, ImportRequest> importRequestCacheConfig = new CacheConfig<>();

        importRequestCacheConfig.setExpireAfterAccess(TimeUnit.HOURS.toMillis(5));
        importRequestCacheConfig.setListener(new ExpiryListener<>() {

            @Override
            public void onExpired(final String key, final ImportRequest importRequest) {
                importRequest.clean();
            }
        });
        this.importRequestCache = CacheManager.getInstance().createCache(importRequestCacheConfig);
    }

    private static class Instance {
        private static final ImportRequestCacheImpl IMPORT_REQUEST_CACHE = new ImportRequestCacheImpl();
    }

    /**
     * Gets the {@link ImportRequestCache} instance
     *
     * @return the {@link ImportRequestCache}
     */
    public static ImportRequestCache getInstance() {
        return ImportRequestCacheImpl.Instance.IMPORT_REQUEST_CACHE;
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the import request ID
     * @return the {@link ImportRequest}
     */
    @Override
    public Optional<ImportRequest> get(final String requestId) {
        return Optional.ofNullable(importRequestCache.get(requestId));
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the import request ID
     * @return the {@link ImportRequest}
     */
    @Override
    public Optional<ImportRequest> poll(final String requestId) {
        return Optional.ofNullable(importRequestCache.remove(requestId));
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the import request ID
     * @param request   The import request to be put into the cache
     */
    @Override
    public void put(final String requestId, final ImportRequest request) {

        if (!importRequestCache.containsKey(requestId)) {
            importRequestCache.put(requestId, request);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public void clear() {
        importRequestCache.clear();
    }
}
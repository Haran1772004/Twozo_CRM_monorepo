package com.tlc.crm.core.features.internal.dataimport.async.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheConfig;
import com.tlc.cache.CacheManager;
import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.features.internal.dataimport.async.req.RevertRequest;
import com.tlc.crm.core.features.internal.dataimport.async.req.RevertRequestCache;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Cache to store the {@link RevertRequest}
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public class RevertRequestCacheImpl implements RevertRequestCache  {

    private final Cache<String, RevertRequest> revertRequestCache;

    private RevertRequestCacheImpl() {
        final CacheConfig<String, RevertRequest> revertRequestCacheConfig = new CacheConfig<>();

        revertRequestCacheConfig.setExpireAfterAccess(TimeUnit.HOURS.toMillis(2));
        this.revertRequestCache = CacheManager.getInstance().createCache(revertRequestCacheConfig);
    }

    private static class Instance {
        private static final RevertRequestCacheImpl REVERT_REQUEST_CACHE = new RevertRequestCacheImpl();
    }

    /**
     * Gets the {@link RevertRequestCache} instance
     *
     * @return the {@link RevertRequestCache}
     */
    public static RevertRequestCache getInstance() {
        return Instance.REVERT_REQUEST_CACHE;
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the revert request ID
     * @return the {@link RevertRequest}
     */
    @Override
    public Optional<RevertRequest> get(final String requestId) {
        return Optional.ofNullable(revertRequestCache.get(requestId));
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the revert request ID
     * @return the {@link RevertRequest}
     */
    @Override
    public Optional<RevertRequest> poll(final String requestId) {
        return Optional.ofNullable(revertRequestCache.remove(requestId));
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the revert request ID
     * @param request   The revert request to be put into the cache
     */
    @Override
    public void put(final String requestId, final RevertRequest request) {

        if (!revertRequestCache.containsKey(requestId)) {
            revertRequestCache.put(requestId, request);
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
        revertRequestCache.clear();
    }
}

package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.features.internal.dataimport.async.impl.ImportRequestCacheImpl;

import java.util.Optional;

/**
 * Cache to store the {@link ImportRequest}
 *
 * @author sundar
 * @version 1.0
 */
public interface ImportRequestCache extends LocalCache {

    /**
     * Gets the {@link ImportRequestCache} instance
     *
     * @return the {@link ImportRequestCache}
     */
    static ImportRequestCache getInstance() {
        return ImportRequestCacheImpl.getInstance();
    }

    /**
     * Gets the {@link ImportRequest} by the given request ID
     *
     * @param requestId Represents the import request ID
     * @return the {@link ImportRequest}
     */
    Optional<ImportRequest> get(final String requestId);

    /**
     * Gets and removes the {@link ImportRequest} by the given request ID
     *
     * @param requestId Represents the import request ID
     * @return the {@link ImportRequest}
     */
    Optional<ImportRequest> poll(final String requestId);

    /**
     * Puts the {@link ImportRequest} into the cache
     *
     * @param requestId Represents the import request ID
     * @param request   The import request to be put into the cache
     */
    void put(final String requestId, final ImportRequest request);
}
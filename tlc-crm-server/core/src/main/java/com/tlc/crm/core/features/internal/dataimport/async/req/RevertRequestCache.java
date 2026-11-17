package com.tlc.crm.core.features.internal.dataimport.async.req;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.features.internal.dataimport.async.impl.RevertRequestCacheImpl;

import java.util.Optional;

/**
 * Cache to store the {@link RevertRequest}
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public interface RevertRequestCache extends LocalCache {

    /**
     * Gets the {@link RevertRequestCache} instance
     *
     * @return the {@link RevertRequestCache}
     */
    static RevertRequestCache getInstance() {
        return RevertRequestCacheImpl.getInstance();
    }

    /**
     * Gets the {@link RevertRequest} by the given request ID
     *
     * @param requestId Represents the revert request ID
     * @return the {@link RevertRequest}
     */
    Optional<RevertRequest> get(final String requestId);

    /**
     * Gets and removes the {@link RevertRequest} by the given request ID
     *
     * @param requestId Represents the revert request ID
     * @return the {@link RevertRequest}
     */
    Optional<RevertRequest> poll(final String requestId);

    /**
     * Puts the {@link RevertRequest} into the cache
     *
     * @param requestId Represents the revert request ID
     * @param request   The revert request to be put into the cache
     */
    void put(final String requestId, final RevertRequest request);
}
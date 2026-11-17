package com.tlc.search.request.index.multi.read;

import com.tlc.search.request.index.IndexRequest;
import com.tlc.search.request.index.multi.read.impl.MultiSingleIndexRequestImpl;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;

import java.util.Collection;

/**
 * <p>
 * Holds multiple {@link SingleIndexSearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface MultiSingleIndexRequest extends IndexRequest {

    /**
     * <p>
     * Get {@link MultiSingleIndexRequest}.
     * </p>
     *
     * @param searchRequests search requests.
     * @return {@link MultiSingleIndexRequest}.
     */
    static MultiSingleIndexRequest get(final Collection<SingleIndexSearchRequest> searchRequests) {
        return new MultiSingleIndexRequestImpl(searchRequests);
    }

    /**
     * <p>
     * Gets collection {@link SingleIndexSearchRequest}.
     * </p>
     *
     * @return collection {@link SingleIndexSearchRequest}.
     */
    Collection<SingleIndexSearchRequest> get();
}
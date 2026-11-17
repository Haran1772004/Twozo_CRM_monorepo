package com.tlc.search.request;

import com.tlc.search.request.index.IndexRequest;

/**
 * <p>
 * Provides the request which holds group of all the single index requests (index, update, delete, etc,...).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SingleIndexRequest extends IndexRequest {

    /**
     * <p>
     * Gets index name.
     * </p>
     *
     * @return index name.
     */
    String getIndexName();
}
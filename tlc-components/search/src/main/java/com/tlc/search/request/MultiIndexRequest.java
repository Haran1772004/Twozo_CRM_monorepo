package com.tlc.search.request;

import com.tlc.search.request.index.IndexRequest;

import java.util.List;

/**
 * <p>
 * Provides the request which holds group of all the multi index requests (index, update, delete, search, etc,...).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface MultiIndexRequest extends IndexRequest {

    /**
     * <p>
     * Gets the index names.
     * </p>
     *
     * @return index names.
     */
    List<String> getIndexNames();
}

package com.tlc.search.request.index.single.read.impl;

import com.dslplatform.json.NonNull;
import com.tlc.search.request.SingleIndexRequest;

/**
 * <p>
 * Provides the request which holds group of all the single index requests (index, update, delete, etc,...).
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractSingleIndexRequest implements SingleIndexRequest {

    private final String indexName;

    protected AbstractSingleIndexRequest(@NonNull final String indexName) {
        this.indexName = indexName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getIndexName() {
        return indexName;
    }
}
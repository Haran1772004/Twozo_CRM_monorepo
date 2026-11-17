package com.tlc.search.request.index.single.read.count;

import com.tlc.search.model.CountRequest;
import com.tlc.search.request.IndexCountRequest;
import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.single.read.count.impl.SingleIndexCountRequestImpl;

/**
 * <p>
 * Provides the request which is used to fetch the count of available documents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface SingleIndexCountRequest extends IndexCountRequest, SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link SingleIndexCountRequest}.
     * </p>
     *
     * @param index Holds the array of indexes, which holds the group of documents.
     * @return {@link SingleIndexCountRequest} that hold the request to count the available documents in the given indexes.
     */
    static SingleIndexCountRequest get(final String index, final CountRequest countRequest) {
        return new SingleIndexCountRequestImpl(index, countRequest);
    }
}
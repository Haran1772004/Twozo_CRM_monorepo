package com.tlc.search.request.index.single.read.count.impl;

import com.tlc.search.model.CountRequest;
import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;
import com.tlc.search.request.index.single.read.count.SingleIndexCountRequest;

/**
 * <p>
 * Provides the request which is used to fetch the count of available documents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SingleIndexCountRequestImpl extends AbstractSingleIndexRequest implements SingleIndexCountRequest {

    private final CountRequest countRequest;

    public SingleIndexCountRequestImpl(final String index, final CountRequest countRequest) {
        super(index);
        this.countRequest = countRequest;
    }

    /**
     * <p>
     * Gets {@link CountRequest}.
     * </p>
     *
     * @return {@link CountRequest}.
     */
    public CountRequest getCountRequest() {
        return countRequest;
    }
}
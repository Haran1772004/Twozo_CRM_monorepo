package com.tlc.search.request.index.multi.read.count.impl;

import com.tlc.search.model.CountRequest;
import com.tlc.search.request.index.multi.read.count.MultiIndexCountRequest;
import com.tlc.search.request.index.multi.read.impl.AbstractMultiIndexRequest;

import java.util.List;

/**
 * <p>
 * Count request to be processed in multiple indexes on a single {@link CountRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class MultiIndexCountRequestImpl extends AbstractMultiIndexRequest implements MultiIndexCountRequest {

    private final CountRequest countRequest;

    public MultiIndexCountRequestImpl(final List<String> indexes, final CountRequest countRequest) {
        super(indexes);
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
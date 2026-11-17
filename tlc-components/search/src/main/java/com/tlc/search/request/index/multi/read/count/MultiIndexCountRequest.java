package com.tlc.search.request.index.multi.read.count;

import com.tlc.search.model.CountRequest;
import com.tlc.search.request.IndexCountRequest;
import com.tlc.search.request.MultiIndexRequest;
import com.tlc.search.request.index.multi.read.count.impl.MultiIndexCountRequestImpl;

import java.util.List;

/**
 * <p>
 * Count request to be processed in multiple indexes on a single {@link CountRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface MultiIndexCountRequest extends IndexCountRequest, MultiIndexRequest {

    /**
     * <p>
     * Gets {@link MultiIndexCountRequest}.
     * </p>
     *
     * @param indexes Index names.
     * @return {@link MultiIndexCountRequest}.
     */
    static MultiIndexCountRequest get(final List<String> indexes, final CountRequest countRequest) {
        return new MultiIndexCountRequestImpl(indexes, countRequest);
    }
}
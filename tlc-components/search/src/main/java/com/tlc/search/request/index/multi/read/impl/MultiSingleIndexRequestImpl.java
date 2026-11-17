package com.tlc.search.request.index.multi.read.impl;

import com.tlc.search.request.index.multi.read.MultiSingleIndexRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;

import java.util.Objects;
import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * Holds multiple {@link SingleIndexSearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class MultiSingleIndexRequestImpl implements MultiSingleIndexRequest {

    private final Collection<SingleIndexSearchRequest> searchRequests;

    public MultiSingleIndexRequestImpl(final Collection<SingleIndexSearchRequest> searchRequests) {
        this.searchRequests = searchRequests;
    }

    /**
     * <p>
     * Gets collection {@link SingleIndexSearchRequest}.
     * </p>
     *
     * @return collection {@link SingleIndexSearchRequest}.
     */
    public Collection<SingleIndexSearchRequest> get() {
        return Objects.isNull(searchRequests) ? Collections.emptyList() : searchRequests;
    }
}
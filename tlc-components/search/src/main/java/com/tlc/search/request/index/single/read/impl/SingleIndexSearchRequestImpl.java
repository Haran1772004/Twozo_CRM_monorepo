package com.tlc.search.request.index.single.read.impl;

import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.single.read.SingleIndexSearchRequest;

/**
 * <p>
 * Provides the request which is used to search the available documents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class SingleIndexSearchRequestImpl extends AbstractSingleIndexRequest implements SingleIndexSearchRequest {

    private final SearchRequest searchRequest;

    public SingleIndexSearchRequestImpl(final String index, final SearchRequest searchRequest) {
        super(index);
        this.searchRequest = searchRequest;
    }

    public SearchRequest getSearchRequest() {
        return searchRequest;
    }
}
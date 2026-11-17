package com.tlc.search.request.index.multi.read.impl;

import com.dslplatform.json.NonNull;
import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.index.multi.read.MultiSearchIndexRequest;

import java.util.List;

/**
 * <p>
 * Multi search operation to be performed on different indexes with a {@link SearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class MultiIndexSearchRequestImpl extends AbstractMultiIndexRequest implements MultiSearchIndexRequest {

    private final SearchRequest searchRequest;

    public MultiIndexSearchRequestImpl(@NonNull final List<String> indexes, @NonNull final SearchRequest searchRequest) {
        super(indexes);
        this.searchRequest = searchRequest;
    }

    /**
     * <p>
     * Gets {@link SearchRequest}.
     * </p>
     *
     * @return collection {@link SearchRequest}.
     */
    public SearchRequest getSearchRequest() {
        return searchRequest;
    }
}

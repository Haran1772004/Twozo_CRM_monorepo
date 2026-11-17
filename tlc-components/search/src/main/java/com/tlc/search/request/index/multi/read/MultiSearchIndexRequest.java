package com.tlc.search.request.index.multi.read;

import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.MultiIndexRequest;
import com.tlc.search.request.index.multi.read.impl.MultiIndexSearchRequestImpl;

import java.util.List;

/**
 * <p>
 * Multi search operation to be performed on different indexes with a {@link SearchRequest}.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface MultiSearchIndexRequest extends MultiIndexRequest {

    /**
     * <p>
     * Get {@link MultiSearchIndexRequest}.
     * </p>
     *
     * @param searchRequest {@link SearchRequest}.
     * @return {@link MultiSearchIndexRequest}.
     */
    static MultiSearchIndexRequest get(final List<String> indexes, final SearchRequest searchRequest) {
        return new MultiIndexSearchRequestImpl(indexes, searchRequest);
    }

    /**
     * <p>
     * Gets {@link SearchRequest}.
     * </p>
     *
     * @return collection {@link SearchRequest}.
     */
    SearchRequest getSearchRequest();
}
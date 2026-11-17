package com.tlc.search.request.index.single.read;

import com.tlc.search.model.SearchRequest;
import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.single.read.impl.SingleIndexSearchRequestImpl;

/**
 * <p>
 * Provides the request which is used to search the available documents.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface SingleIndexSearchRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of the {@link SingleIndexSearchRequest}.
     * </p>
     *
     * @param index Holds the array of index, which holds the group of documents.
     * @return {@link SingleIndexSearchRequest} that holds the details for searching the documents.
     */
    static SingleIndexSearchRequest get(final String index, final SearchRequest searchRequest) {
        return new SingleIndexSearchRequestImpl(index, searchRequest);
    }

    SearchRequest getSearchRequest();
}
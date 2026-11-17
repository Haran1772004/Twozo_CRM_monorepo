package com.tlc.search.request;

import com.tlc.search.model.CountRequest;

/**
 * <p>
 * Request depicting requirements for documents count in a index.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface IndexCountRequest {

    /**
     * <p>
     * Gets {@link CountRequest}.
     * </p>
     *
     * @return {@link CountRequest}.
     */
    CountRequest getCountRequest();
}

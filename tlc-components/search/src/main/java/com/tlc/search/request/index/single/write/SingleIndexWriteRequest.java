package com.tlc.search.request.index.single.write;

import com.tlc.search.request.SingleIndexRequest;

/**
 * <p>
 * Provides the request which is used to manipulate the data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface SingleIndexWriteRequest extends SingleIndexRequest {

    /**
     * <p>
     * Id of a document.
     * </p>
     *
     * @return id of a document.
     */
    String id();
}

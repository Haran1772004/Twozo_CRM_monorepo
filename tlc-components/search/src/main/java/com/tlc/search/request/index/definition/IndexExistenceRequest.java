package com.tlc.search.request.index.definition;

import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.definition.impl.IndexExistenceRequestImpl;

/**
 * <p>
 * Request to check whether the index exists.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexExistenceRequest extends SingleIndexRequest {

    /**
     * <p>
     * Gets {@link IndexExistenceRequest}.
     * </p>
     *
     * @param index Index name.
     * @return {@link IndexExistenceRequest}.
     */
    static IndexExistenceRequest get(final String index) {
        return new IndexExistenceRequestImpl(index);
    }
}

package com.tlc.search.request.index.settings;

import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.settings.impl.IndexCloneRequestImpl;

/**
 * <p>
 * Provides the request which is used to clone the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexCloneRequest extends SingleIndexRequest {

    /**
     * <p>
     * Provides the instance of {@link IndexCloneRequest}.
     * </p>
     *
     * @param index       Refers the name which holds the group of documents.
     * @param targetIndex An index which is used to store the copy of the given index.
     * @return {@link IndexCloneRequest} that hold the request to clone the given index.
     */
    static IndexCloneRequest get(final String index, final String targetIndex) {
        return new IndexCloneRequestImpl(index, targetIndex);
    }

    /**
     * <p>
     * Provides the target index which is used to clone the given index.
     * </p>
     *
     * @return Target index.
     */
    String getTargetIndexName();
}

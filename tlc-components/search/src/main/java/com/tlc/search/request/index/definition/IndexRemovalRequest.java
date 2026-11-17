package com.tlc.search.request.index.definition;

import com.tlc.search.request.SingleIndexRequest;
import com.tlc.search.request.index.definition.impl.IndexRemovalRequestImpl;

/**
 * <p>
 * Request to remove the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexRemovalRequest extends SingleIndexRequest {

    /**
     * <p>
     * Gets {@link IndexRemovalRequest}.
     * </p>
     *
     * @param index Index name.
     * @return {@link IndexRemovalRequest}.
     */
    static IndexRemovalRequest get(final String index) {
        return new IndexRemovalRequestImpl(index);
    }
}

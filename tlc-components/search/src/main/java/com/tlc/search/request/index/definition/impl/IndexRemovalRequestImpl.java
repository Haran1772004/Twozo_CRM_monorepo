package com.tlc.search.request.index.definition.impl;

import com.tlc.search.request.index.definition.IndexRemovalRequest;
import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;

/**
 * <p>
 * Request to remove the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class IndexRemovalRequestImpl extends AbstractSingleIndexRequest implements IndexRemovalRequest {

    public IndexRemovalRequestImpl(final String index) {
        super(index);
    }
}

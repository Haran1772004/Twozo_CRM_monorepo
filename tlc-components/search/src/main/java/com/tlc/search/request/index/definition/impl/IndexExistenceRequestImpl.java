package com.tlc.search.request.index.definition.impl;

import com.tlc.search.request.index.definition.IndexExistenceRequest;
import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;

/**
 * <p>
 * Request to check whether the index exists.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class IndexExistenceRequestImpl extends AbstractSingleIndexRequest implements IndexExistenceRequest {

    public IndexExistenceRequestImpl(final String index) {
        super(index);
    }
}

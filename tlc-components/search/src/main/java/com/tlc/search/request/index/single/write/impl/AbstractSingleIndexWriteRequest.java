package com.tlc.search.request.index.single.write.impl;

import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;
import com.tlc.search.request.index.single.write.SingleIndexWriteRequest;

/**
 * <p>
 * Provides the request which is used to manipulate the data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractSingleIndexWriteRequest extends AbstractSingleIndexRequest implements SingleIndexWriteRequest {

    private final String id;

    protected AbstractSingleIndexWriteRequest(final String id, final String index) {
        super(index);
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String id() {
        return id;
    }
}

package com.tlc.search.request.index.settings.impl;

import com.tlc.search.request.index.single.read.impl.AbstractSingleIndexRequest;
import com.tlc.search.request.index.settings.IndexCloneRequest;

/**
 * <p>
 * Provides the request which is used to clone the index.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class IndexCloneRequestImpl extends AbstractSingleIndexRequest implements IndexCloneRequest {

    private final String targetIndex;

    public IndexCloneRequestImpl(final String index, final String targetIndex) {
        super(index);
        this.targetIndex = targetIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTargetIndexName() {
        return targetIndex;
    }
}

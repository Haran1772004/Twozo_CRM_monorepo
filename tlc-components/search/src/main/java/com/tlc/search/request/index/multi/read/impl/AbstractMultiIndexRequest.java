package com.tlc.search.request.index.multi.read.impl;

import com.tlc.search.request.MultiIndexRequest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Abstract request containing required details for multi-index actions.
 * </p>
 *
 * @author Sathish.
 * @version 1.0.
 */
public abstract class AbstractMultiIndexRequest implements MultiIndexRequest {

    private final List<String> indexNames;

    protected AbstractMultiIndexRequest(final List<String> indexNames) {
        this.indexNames = indexNames;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<String> getIndexNames() {

        return Objects.isNull(indexNames) ? Collections.emptyList() : indexNames;
    }
}
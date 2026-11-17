package com.tlc.search.request.index.single.write.impl;

import com.tlc.search.request.index.single.write.RestoreRequest;

/**
 * <p>
 * Provides the request which is used to restore the deleted document.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RestoreRequestImpl extends AbstractSingleIndexWriteRequest implements RestoreRequest {

    public RestoreRequestImpl(final String id, final String index) {
        super(id, index);
    }
}

package com.tlc.search.request.index.single.write.impl;

import com.tlc.search.request.index.single.write.RemoveRequest;

/**
 * <p>
 * Provides the request which is used to remove the document.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class RemoveRequestImpl extends AbstractSingleIndexWriteRequest implements RemoveRequest {

    public RemoveRequestImpl(final String id, final String index) {
        super(id, index);
    }
}

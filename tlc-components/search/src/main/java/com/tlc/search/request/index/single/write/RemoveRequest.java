package com.tlc.search.request.index.single.write;

import com.tlc.search.request.index.single.write.impl.RemoveRequestImpl;

/**
 * <p>
 * Provides the request which is used to remove the document.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RemoveRequest extends SingleIndexWriteRequest {

    /**
     * <p>
     * Provides the instance of {@link RemoveRequest}.
     * </p>
     *
     * @param id    Primary value to remove the document.
     * @param index Refers the name which holds the group of documents.
     * @return {@link RemoveRequest} that hold the request to remove the document.
     */
    static RemoveRequest get(final String id, final String index) {
        return new RemoveRequestImpl(id, index);
    }
}

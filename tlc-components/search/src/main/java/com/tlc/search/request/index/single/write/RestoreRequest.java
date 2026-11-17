package com.tlc.search.request.index.single.write;

import com.tlc.search.request.index.single.write.impl.RestoreRequestImpl;

/**
 * <p>
 * Provides the request which is used to restore the deleted document.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface RestoreRequest extends SingleIndexWriteRequest {

    /**
     * <p>
     * Provides the instance of {@link RestoreRequest}.
     * </p>
     *
     * @param id    Primary value to restore the deleted document.
     * @param index Refers the name which holds the group of documents.
     * @return {@link RestoreRequest} that hold the request to restore the deleted data.
     */
    static RestoreRequest get(final String id, final String index) {
        return new RestoreRequestImpl(id, index);
    }
}

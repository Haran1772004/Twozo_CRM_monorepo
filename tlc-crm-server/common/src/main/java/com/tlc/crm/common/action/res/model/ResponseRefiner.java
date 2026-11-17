package com.tlc.crm.common.action.res.model;

import com.tlc.commons.json.JsonObject;

/**
 * A functional interface for performing final modifications to the API response payload.
 * <p>
 * Implementations of this interface are typically invoked during the "pre-send" phase of the
 * request lifecycle. They provide a hook to sanitize, filter, or augment the outgoing
 * JSON data (e.g., removing hidden fields based on permissions) immediately before it is
 * written to the network response.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@FunctionalInterface
public interface ResponseRefiner {

    /**
     * Executes the finalization logic on the provided response JSON.
     * <p>
     * This method is intended to mutate the passed {@code jsonObject} in-place.
     * Common operations include removing keys, adding metadata, or masking sensitive values.
     * </p>
     *
     * @param jsonObject the mutable JSON body of the API response to be processed.
     */
    void refine(final JsonObject jsonObject);
}
package com.tlc.validator.model;

import com.tlc.commons.code.ErrorCode;

/**
 * <p>
 * A record containing error response with relevant details.
 * This can be used to provide structured error messages in API responses
 * </p>
 *
 * @param errorCode Represents the {@link ErrorCode}
 * @param message   Represents the short message describing the error
 * @param details   Represents the more detailed explanation of the error, often useful for debugging.
 * @param target    Represents the specific target associated with the error
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public record ErrorDescriptor(ErrorCode errorCode, String message, String details, String target) {

    public ErrorDescriptor(final ErrorCode errorCode, final String details, final String target) {
        this(errorCode, errorCode.getMessage(), details, target);
    }

    public ErrorDescriptor(final ErrorCode errorCode, final String details) {
        this(errorCode, errorCode.getMessage(), details, null);
    }

    public ErrorDescriptor(final ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null, null);
    }
}
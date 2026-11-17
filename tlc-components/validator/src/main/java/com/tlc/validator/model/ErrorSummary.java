package com.tlc.validator.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * A record representing an error response with detailed information.
 * This record is typically used to structure error messages in API responses,
 * ensuring consistency and ease of debugging.
 * </p>
 *
 * @param errorDescriptor Represents the {@link ErrorDescriptor}
 * @param errors          Represents the collection of error messages
 * @param timestamp       Represents the error logged time stamp
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public record ErrorSummary(ErrorDescriptor errorDescriptor, Collection<ErrorDescriptor> errors, Long timestamp) {

    public ErrorSummary(final ErrorDescriptor errorDescriptor, final Collection<ErrorDescriptor> errors) {
        this(errorDescriptor, errors, Instant.now().toEpochMilli());
    }

    public ErrorSummary(final ErrorDescriptor errorDescriptor) {
        this(errorDescriptor, Collections.emptyList());
    }
}
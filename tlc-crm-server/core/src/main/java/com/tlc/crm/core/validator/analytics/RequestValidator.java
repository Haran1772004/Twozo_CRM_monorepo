package com.tlc.crm.core.validator.analytics;

import lombok.NonNull;

import java.util.function.Consumer;

/**
 * <p>
 * Provides an abstract way to validate the request configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface RequestValidator<T>
{
    /**
     * <p>
     * Validates a request based on the provided parameters (synchronous)
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param request Represents the request data of type
     */
    void validateRequest( final @NonNull Long orgId,
                          final @NonNull Long userId,
                          final @NonNull T request ) throws Exception;

    /**
     * <p>
     * Validates a request asynchronously. The completionHandler is called with
     * {@code null} on success, or with the exception on failure.
     * Default implementation delegates to the sync version.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param userId             Represents the user id
     * @param request            Represents the request data of type
     * @param completionHandler  Called with null on success, or Exception on failure
     */
    default void validateRequest( final @NonNull Long orgId,
                                  final @NonNull Long userId,
                                  final @NonNull T request,
                                  final @NonNull Consumer<Exception> completionHandler )
    {
        try
        {
            validateRequest( orgId, userId, request );
            completionHandler.accept( null );
        }
        catch ( final Exception exception )
        {
            completionHandler.accept( exception );
        }
    }
}

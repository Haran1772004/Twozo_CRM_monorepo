package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

import java.util.function.Consumer;

/**
 * <p>
 * Provides an abstract way to validate the view apply configuration
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 * @see RequestValidator
 */
public class ViewApplyValidator implements RequestValidator<JsonObject>
{
    private final ViewValidator viewValidator;

    private static class Instance
    {
        private static final ViewApplyValidator INSTANCE = new ViewApplyValidator();
    }

    private ViewApplyValidator()
    {
        this.viewValidator = ViewValidator.INSTANCE();
    }

    public static ViewApplyValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Synchronous validation is not supported because {@link ViewValidator} requires
     * async data fetching. Use {@link #validateRequest(Long, Long, JsonObject, Consumer)} instead.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param userId      Represents the user id
     * @param jsonObject  Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject) throws Exception
    {
        // No-op: view validation requires async report field fetching.
        // Callers must use the async overload with Consumer<Exception>.
    }

    /**
     * Validates the view apply configuration asynchronously by delegating to {@link ViewValidator}.
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param jsonObject        Represents the {@link JsonObject} containing the configuration
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject,
                                 final @NonNull Consumer<Exception> completionHandler )
    {
        viewValidator.validateRequest( orgId, userId, jsonObject, completionHandler );
    }
}

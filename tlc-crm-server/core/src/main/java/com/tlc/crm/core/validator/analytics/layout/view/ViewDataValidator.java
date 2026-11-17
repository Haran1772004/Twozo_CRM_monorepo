package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.crm.common.util.NumberUtils;
import lombok.NonNull;

import java.util.function.Consumer;

/**
 * <p>
 * Provides an abstract way to validate the view data configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class ViewDataValidator implements RequestValidator<JsonObject>
{
    private final int viewNameLimit;
    private final int viewPositionLimit;
    private final ViewValidator viewValidator;

    private static class Instance
    {
        private static final ViewDataValidator INSTANCE = new ViewDataValidator();
    }

    private ViewDataValidator()
    {
        final Configurations configuration = ConfHolder.getInstance()
                                                       .getProperties( CRMReportKeys.ANALYTICS )
                                                       .getNestedConfigs( CRMReportKeys.VIEW );
        this.viewNameLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.NAME_LIMIT ) )
                                        .intValue();
        this.viewPositionLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.VIEW_POSITION_LIMIT ) )
                                            .intValue();
        this.viewValidator = ViewValidator.INSTANCE();
    }

    public static ViewDataValidator INSTANCE()
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
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {

        final long viewPosition = jsonObject.getLong( CRMChildViewKeys.VIEW_POSITION );

        if( 0 >= viewPosition || viewPositionLimit < viewPosition )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_POSITION,
                                 "i18n.analytics.config.invalid.view.position" );
        }
    }

    /**
     * Validates the view data configuration asynchronously.
     * First validates position synchronously, then delegates to {@link ViewValidator} for async validation.
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
        try
        {
            final long viewPosition = jsonObject.getLong( CRMChildViewKeys.VIEW_POSITION );

            if( 0 >= viewPosition || viewPositionLimit < viewPosition )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_POSITION,
                                     "i18n.analytics.config.invalid.view.position" );
            }

            viewValidator.validateRequest( orgId, userId, jsonObject, completionHandler );
        }
        catch( final Exception exception )
        {
            completionHandler.accept( exception );
        }
    }
}

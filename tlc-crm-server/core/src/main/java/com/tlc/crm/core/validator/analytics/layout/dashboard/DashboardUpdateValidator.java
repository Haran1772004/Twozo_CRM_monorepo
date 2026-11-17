package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;
import com.tlc.crm.core.validator.analytics.Validator;

import com.tlc.crm.common.util.NumberUtils;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * <p>
 * Provides an abstract way to validate the dashboard layout update configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class DashboardUpdateValidator implements RequestValidator<JsonObject>
{
    private static final int DASHBOARD_REQUIRED_VIEW_LIMIT = 1;

    private final CRMViewManagerAPI crmViewManager;
    private final Validator validator;
    private final int dashboardNameLimit;
    private final int dashboardDescriptionLimit;
    private final int dashboardViewLimit;

    private static class Instance {
        private static final DashboardUpdateValidator INSTANCE = new DashboardUpdateValidator();
    }

    private DashboardUpdateValidator()
    {
        final Configurations configuration = ConfHolder.getInstance()
                                                       .getProperties( CRMReportKeys.ANALYTICS )
                                                       .getNestedConfigs( CRMReportKeys.DASHBOARD );
        this.dashboardNameLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.NAME_LIMIT) )
                                             .intValue();
        this.dashboardDescriptionLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.DESCRIPTION_LIMIT) )
                                                    .intValue();
        this.dashboardViewLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.VIEW_LIMIT) )
                                             .intValue();
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
        this.validator = Validator.INSTANCE();
    }

    public static DashboardUpdateValidator INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId, final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
        final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager = crmViewManager.getCRMDashboardManager();
        final Long dashboardId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );

        if( !crmDashboardManager.exists( orgId, dashboardId ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
        final CRMDashboardDetails crmDashboardDetails = crmDashboardManager.get( orgId, dashboardId );
        final Collection<CRMChildViewDetails> childViews = crmDashboardDetails.getChildViews();
        final Collection<Long> viewIds = childViews.stream()
                                                   .map( CRMChildViewDetails::id )
                                                   .collect( Collectors.toSet() );

        final String dashboardName = jsonObject.getString( CRMDashboardKeys.DASHBOARD_NAME );

        if( Objects.isNull( dashboardName ) || dashboardName.isBlank() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_SHOULD_NOT_BE_EMPTY,
                         "i18n.analytics.config.dashboard.invalid.name" );
        }

        if( dashboardNameLimit < dashboardName.length() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_TOO_LONG,
                         "i18n.analytics.config.dashboard.name.limit.exceeded" );
        }

        if( jsonObject.containsKey( CRMDashboardKeys.DESCRIPTION ) )
        {
            final String description = jsonObject.getString( CRMDashboardKeys.DESCRIPTION );

            if( Objects.nonNull( description ) && dashboardDescriptionLimit < description.length() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_DESCRIPTION_TOO_LONG,
                             "i18n.analytics.config.dashboard.description.limit.exceeded" );
            }
        }

        if( jsonObject.containsKey( CRMDashboardKeys.CHILD_VIEW ) )
        {
            final JsonArray jsonArray = jsonObject.getJsonArray( CRMDashboardKeys.CHILD_VIEW );
            final int totalViews = jsonArray.size();

            if( dashboardViewLimit < totalViews )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_VIEW_LIMIT_EXCEEDED,
                                     "i18n.analytics.config.dashboard.limit.exceeded" );
            }
            final Collection<Long> positionIndexes = new HashSet<>();
            final RequestValidator<JsonObject> viewDataValidator = validator.viewDataValidator();

            for( int index = 0; index < totalViews; index++ )
            {
                final JsonObject childJson = jsonArray.getJsonObject( index );
                final Long positionIndex = childJson.getLong( CRMChildViewKeys.VIEW_POSITION );

                if( positionIndexes.contains( positionIndex ) )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.VIEW_POSITION_ALREADY_EXISTS,
                                         "i18n.analytics.config.view.position.exists" );
                }

                if( childJson.containsKey( CRMReportKeys.VIEW_ID ) && ( viewIds.isEmpty() || !viewIds.contains( childJson.getLong( CRMReportKeys.VIEW_ID ) ) ) )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                         "i18n.analytics.config.view.invalid.id" );
                }

                positionIndexes.add( positionIndex );
                viewDataValidator.validateRequest( orgId, userId, childJson );
            }
        }
    }

    /**
     * Validates the dashboard update configuration asynchronously.
     * Performs synchronous checks first (dashboard existence, name, description, positions),
     * then delegates child view validation to the view data validator's async method.
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param jsonObject        Represents the {@link JsonObject} containing the configuration
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    public void validateRequest( final @NonNull Long orgId, final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject,
                                 final @NonNull Consumer<Exception> completionHandler )
    {
        try
        {
            final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager = crmViewManager.getCRMDashboardManager();
            final Long dashboardId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );

            if( !crmDashboardManager.exists( orgId, dashboardId ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                     "i18n.analytics.config.dashboard.invalid.id" );
            }
            final CRMDashboardDetails crmDashboardDetails = crmDashboardManager.get( orgId, dashboardId );
            final Collection<CRMChildViewDetails> childViews = crmDashboardDetails.getChildViews();
            final Collection<Long> viewIds = childViews.stream()
                                                       .map( CRMChildViewDetails::id )
                                                       .collect( Collectors.toSet() );

            final String dashboardName = jsonObject.getString( CRMDashboardKeys.DASHBOARD_NAME );

            if( Objects.isNull( dashboardName ) || dashboardName.isBlank() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_SHOULD_NOT_BE_EMPTY,
                             "i18n.analytics.config.dashboard.invalid.name" );
            }

            if( dashboardNameLimit < dashboardName.length() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_TOO_LONG,
                             "i18n.analytics.config.dashboard.name.limit.exceeded" );
            }

            if( jsonObject.containsKey( CRMDashboardKeys.DESCRIPTION ) )
            {
                final String description = jsonObject.getString( CRMDashboardKeys.DESCRIPTION );

                if( Objects.nonNull( description ) && dashboardDescriptionLimit < description.length() )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_DESCRIPTION_TOO_LONG,
                                 "i18n.analytics.config.dashboard.description.limit.exceeded" );
                }
            }

            if( jsonObject.containsKey( CRMDashboardKeys.CHILD_VIEW ) )
            {
                final JsonArray jsonArray = jsonObject.getJsonArray( CRMDashboardKeys.CHILD_VIEW );
                final int totalViews = jsonArray.size();

                if( dashboardViewLimit < totalViews )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_VIEW_LIMIT_EXCEEDED,
                                         "i18n.analytics.config.dashboard.limit.exceeded" );
                }
                final Collection<Long> positionIndexes = new HashSet<>();
                final RequestValidator<JsonObject> viewDataValidator = validator.viewDataValidator();

                for( int index = 0; index < totalViews; index++ )
                {
                    final JsonObject childJson = jsonArray.getJsonObject( index );
                    final Long positionIndex = childJson.getLong( CRMChildViewKeys.VIEW_POSITION );

                    if( positionIndexes.contains( positionIndex ) )
                    {
                        throw ErrorCode.get( AnalyticsErrorCodes.VIEW_POSITION_ALREADY_EXISTS,
                                             "i18n.analytics.config.view.position.exists" );
                    }

                    if( childJson.containsKey( CRMReportKeys.VIEW_ID ) && ( viewIds.isEmpty() || !viewIds.contains( childJson.getLong( CRMReportKeys.VIEW_ID ) ) ) )
                    {
                        throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                             "i18n.analytics.config.view.invalid.id" );
                    }

                    positionIndexes.add( positionIndex );
                }

                // Chain async validations sequentially for each child view
                validateChildViewsAsync( orgId, userId, jsonArray, 0, totalViews, viewDataValidator, completionHandler );
            }
            else
            {
                completionHandler.accept( null );
            }
        }
        catch( final Exception exception )
        {
            completionHandler.accept( exception );
        }
    }

    /**
     * Recursively validates each child view asynchronously by chaining callbacks.
     *
     * @param orgId              the organization ID
     * @param userId             the user ID
     * @param jsonArray          the array of child view JSON objects
     * @param index              the current index being validated
     * @param totalViews         the total number of child views
     * @param viewDataValidator  the validator for each child view
     * @param completionHandler  called with {@code null} when all views pass, or with the first exception
     */
    private void validateChildViewsAsync( final Long orgId, final Long userId,
                                          final JsonArray jsonArray, final int index, final int totalViews,
                                          final RequestValidator<JsonObject> viewDataValidator,
                                          final Consumer<Exception> completionHandler )
    {
        if( index >= totalViews )
        {
            completionHandler.accept( null );
            return;
        }

        final JsonObject childJson = jsonArray.getJsonObject( index );
        viewDataValidator.validateRequest( orgId, userId, childJson, ( error ) ->
        {
            if( error != null )
            {
                completionHandler.accept( error );
            }
            else
            {
                validateChildViewsAsync( orgId, userId, jsonArray, index + 1, totalViews, viewDataValidator, completionHandler );
            }
        } );
    }
}
package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMFavourite;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout favourite configuration
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see RequestValidator
 */
public class FavouriteValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private FavouriteValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final FavouriteValidator INSTANCE = new FavouriteValidator();
    }

    public static FavouriteValidator INSTANCE()
    {
        return FavouriteValidator.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param userId     Represents the user id
     * @param jsonObject Represents the {@link JsonObject} containing the configuration
     */
    @Override
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
        final CRMViewConfigManager<CRMFavourite> crmFavouriteManager = crmViewManager.getCRMFavouriteManager();
        final boolean isFavouriteView = jsonObject.optBoolean( CRMDashboardKeys.IS_FAVOURITE, false );
        final Long dashboardId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );
        final Table favouriteTable = crmFavouriteManager.getTable();
        final Collection<CRMFavourite> crmFavourites = crmFavouriteManager.getAll( orgId,
                                                                                   List.of( Criteria.eq( favouriteTable.getColumn( CRMFAVOURITEVIEW.CRM_DASHBOARD_ID ),
                                                                                                         dashboardId ),
                                                                                            Criteria.eq( favouriteTable.getColumn( CRMFAVOURITEVIEW.USER_ID ),
                                                                                                         userId ) ) );
        if ( !crmViewManager.getCRMDashboardManager()
                            .exists( orgId, dashboardId ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                         "i18n.analytics.config.dashboard.invalid.id" );
        }

        if ( isFavouriteView )
        {
            if ( !crmFavourites.isEmpty() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_ALREADY_MARKED_FAVOURITE,
                             "i18n.analytics.config.dashboard.already.favourite" );
            }
        }
        else if ( crmFavourites.isEmpty() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NOT_FAVOURITE_YET,
                         "i18n.analytics.config.dashboard.not.favourite.yet" );
        }
    }
}

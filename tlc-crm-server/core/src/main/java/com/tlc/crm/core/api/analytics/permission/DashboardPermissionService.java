package com.tlc.crm.core.api.analytics.permission;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.ReportField;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMFavourite;
import com.tlc.crm.core.api.analytics.permission.handler.AnalyticsPermissionHandler;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Service class responsible for managing dashboard permissions and access controls.
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class DashboardPermissionService
{
    private final CRMViewManagerAPI crmViewManager;
    private final CRMViewConfigManager<CRMFavourite> crmFavouriteManager;
    private final AnalyticsServiceAPI analyticsServiceAPI;
    private final AnalyticsPermissionHandler analyticsPermissionHandler;

    private DashboardPermissionService()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
        this.crmFavouriteManager = crmViewManager.getCRMFavouriteManager();
        this.analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        this.analyticsPermissionHandler = AnalyticsPermissionHandler.getInstance();
    }

    private static class Instance
    {
        private static final DashboardPermissionService INSTANCE = new DashboardPermissionService();
    }

    public static DashboardPermissionService INSTANCE()
    {
        return DashboardPermissionService.Instance.INSTANCE;
    }

    /**
     * Checks if the specified dashboard is marked as a favorite by the current user.
     *
     * @param orgId       The organization ID (must not be null).
     * @param dashboardId The ID of the dashboard to check (must not be null).
     * @return {@code true} if the dashboard is a favorite, {@code false} otherwise.
     */
    public boolean isFavourite(final @NonNull Long orgId, final @NonNull Long dashboardId )
    {
        final Table crmFavouriteTable = crmFavouriteManager.getTable();

        try {
            final Collection<CRMFavourite> crmFavourites = crmFavouriteManager.getAll( orgId, List.of( Criteria.eq( crmFavouriteTable.getColumn( CRMFAVOURITEVIEW.USER_ID ),
                                                                                                                    getCurrentUserId() ),
                                                                                                       Criteria.eq( crmFavouriteTable.getColumn( CRMFAVOURITEVIEW.CRM_DASHBOARD_ID ),
                                                                                                                    dashboardId ) ) );
            return !crmFavourites.isEmpty();
        }
        catch ( Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.FAVORITES_ACTION_FAILED,
                         "i18n.analytics.favourite.action.failed" );
        }
    }

    /**
     * Determines if the current user has permission to can clone the dashboard.
     *
     * @return {@code true} if the current user is the owner and can clone the dashboard, otherwise {@code false}.
     */
    public boolean canClone()
    {
        return true;
    }

    /**
     * Determines if the current user has permission to move a dashboard to the trash.
     *
     * @param creatorUserId The ID of the dashboard's creator.
     * @param isCurated     If the current dashboard is curated or not.
     * @return {@code true} if the current user is the owner and can trash the dashboard, otherwise {@code false}.
     */
    public boolean canTrash( final @NonNull Long creatorUserId, final boolean isCurated )
    {
        return !isCurated && isOwner( creatorUserId );
    }

    /**
     * Determines if the current user has permission to move a dashboard to the restore.
     *
     * @param creatorUserId The ID of the dashboard's creator.
     * @return {@code true} if the current user is the owner and can restore the dashboard, otherwise {@code false}.
     */
    public boolean canRestore(final @NonNull Long creatorUserId )
    {
        return isOwner( creatorUserId );
    }

    /**
     * Determines if the current user has permission to move a dashboard to delete.
     *
     * @param creatorUserId The ID of the dashboard's creator.
     * @return {@code true} if the current user is the owner and can delete the dashboard, otherwise {@code false}.
     */
    public boolean canDelete(final @NonNull Long creatorUserId )
    {
        return isOwner( creatorUserId );
    }

    /**
     * Retrieves a map containing dashboard access permissions for a given organization.
     *
     * @param orgId   The organization ID (must not be null).
     * @param isTrash Indicates whether the dashboards are in the trash.
     * @param data    A map containing dashboard IDs as keys and creator user IDs as values (must not be null).
     * @return A map of dashboard permissions.
     */
    public Map<Long, Map<String, Boolean>> getDashboardPermissionAccess( final @NonNull Long orgId,
                                                                         final boolean isTrash,
                                                                         final @NonNull Map<Long, Pair<Long,Boolean>> data )
    {
        final Map<Long, Boolean> favouriteMap = fetchFavouriteDashboard( orgId, data.keySet() );

        return data.entrySet()
                   .stream()
                   .collect( Collectors.toMap( Map.Entry::getKey,
                                               entry ->
                                               {
                                                   final Long dashboardId = entry.getKey();
                                                   final Pair<Long, Boolean> entryValue = entry.getValue();
                                                   final Map<String, Boolean> accessMap = new HashMap<>();
                                                   final Long creatorId = entryValue.getKey();

                                                   accessMap.put( CRMDashboardKeys.IS_FAVOURITE,
                                                                  favouriteMap.getOrDefault( dashboardId, false ) );
                                                   accessMap.put( CRMDashboardKeys.CAN_CLONE, canClone(orgId, dashboardId) );

                                                   if ( isTrash )
                                                   {
                                                       accessMap.put( CRMDashboardKeys.CAN_RESTORE, canRestore( creatorId ) );
                                                       accessMap.put( CRMDashboardKeys.CAN_DELETE, canDelete( creatorId ) );
                                                   }
                                                   else
                                                   {
                                                       accessMap.put( CRMDashboardKeys.CAN_MOVE_TO_TRASH, canTrash( creatorId, entryValue.getValue() ) );
                                                   }
                                                   return accessMap;
                                               } ) );
    }

    /**
     * Fetches favorite dashboards for the current user within the given organization.
     *
     * @param orgId        The organization ID.
     * @param dashboardIds A set of dashboard IDs to check for favorites.
     * @return A map where the key is the dashboard ID and the value is {@code true} if it's a favorite.
     */
    private Map<Long, Boolean> fetchFavouriteDashboard( final Long orgId, final Set<Long> dashboardIds )
    {
        final Table crmFavouriteTable = crmFavouriteManager.getTable();

        try
        {
            final Collection<CRMFavourite> crmFavourites = crmFavouriteManager.getAll( orgId, List.of( Criteria.eq( crmFavouriteTable.getColumn( CRMFAVOURITEVIEW.USER_ID ),
                                                                                                                    getCurrentUserId() ),
                                                                                                       Criteria.in( crmFavouriteTable.getColumn( CRMFAVOURITEVIEW.CRM_DASHBOARD_ID ),
                                                                                                                    dashboardIds ) ) );

            return crmFavourites.isEmpty() ? Collections.emptyMap() : crmFavourites.stream()
                                                                                   .collect( Collectors.toMap( CRMFavourite::getDashboardId, fav -> true ) );
        }
        catch ( Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.FAVORITES_ACTION_FAILED,
                         "i18n.analytics.favourite.action.failed" );
        }
    }

    /**
     * Checks if the given user ID matches the currently logged-in user's ID.
     *
     * @param creatorUserId The ID of the dashboard creator.
     * @return {@code true} if the current user is the owner, otherwise {@code false}.
     */
    private boolean isOwner( final Long creatorUserId )
    {
        return getCurrentUserId().equals( creatorUserId );
    }

    /**
     * Retrieves the ID of the currently logged-in user.
     *
     * @return The ID of the logged-in user.
     */
    private Long getCurrentUserId()
    {
        return LoggedInUserInfo.getUser()
                               .id();
    }

    /**
     * Checks if the given dashboard can be cloned for the current user within the given organization.
     *
     * @param orgId        The organization ID.
     * @param dashboardId  dashboard ID to check whether it can be cloned.
     * @return true if the dashboard can be cloned else return false
     */
    private boolean canClone(final Long orgId, final Long dashboardId) {
        final Collection<CRMChildViewDetails> crmChildViewDetails = crmViewManager.getCRMDashboardManager()
                .get(orgId, dashboardId).getCrmChildViews();

        if (Objects.isNull(crmChildViewDetails)) {
            return true;
        }

        final Collection<Long> anaViewIds = crmChildViewDetails.stream().map(CRMChildViewDetails::getCrmViewMapper)
                .flatMap(Collection::stream).map(CRMANAViewMapper::getAnaViewId).toList();
        final Collection<ChartView> models = analyticsServiceAPI.getChartLayout().get(orgId, anaViewIds);
        final Collection<Long> fieldIds = models.stream().map(ChartView::getReportFields).flatMap(Collection::stream)
                .map(ReportField::getFieldId).collect(Collectors.toSet());

        return analyticsPermissionHandler.isClonable(orgId, LoggedInUserInfo.getUser().getRoleId(), fieldIds);
    }

    /**
     * <p>
     * Determines if the current user has permission to export the specified dashboard.
     * </p>
     *
     * <p>
     * Returns {@code true} immediately if the dashboard has no child views (no restrictions).
     * </p>
     * @param orgId                The organization ID.
     * @param dashboardId          The ID of the dashboard to check for export capability.
     * @param analyticsRequest     The analytics request containing user role and permission information.
     * @return {@code true} if the current user has permission to export the dashboard and all its fields,
     *         {@code false} otherwise.
     */
    public boolean canExport(final @NonNull Long orgId, final @NonNull Long dashboardId, final @NonNull AnalyticsRequest analyticsRequest) {
        final Collection<CRMChildViewDetails> crmChildViewDetails = crmViewManager.getCRMDashboardManager()
                .get(orgId, dashboardId).getCrmChildViews();

        if (Objects.isNull(crmChildViewDetails)) {
            return true;
        }

        final Collection<Long> anaViewIds = crmChildViewDetails.stream().map(CRMChildViewDetails::getCrmViewMapper)
                .flatMap(Collection::stream).map(CRMANAViewMapper::getAnaViewId).toList();
        final Collection<ChartView> models = analyticsServiceAPI.getChartLayout().get(orgId, anaViewIds);
        final Collection<Long> fieldIds = models.stream().map(ChartView::getReportFields).flatMap(Collection::stream)
                .map(ReportField::getFieldId).collect(Collectors.toSet());

        return analyticsPermissionHandler.isExportable(fieldIds, analyticsRequest);
    }
}

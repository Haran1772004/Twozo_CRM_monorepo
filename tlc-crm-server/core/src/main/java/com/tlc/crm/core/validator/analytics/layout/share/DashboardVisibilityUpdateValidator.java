package com.tlc.crm.core.validator.analytics.layout.share;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.share.DashboardVisibility;
import com.tlc.crm.core.api.analytics.entity.share.Visibility;
import com.tlc.crm.core.api.analytics.entity.share.VisibilityPermission;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.sql.common.api.dml.Criteria;

import lombok.NonNull;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout update configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
@Accessors( fluent = true )
class DashboardVisibilityUpdateValidator implements RequestValidator<JsonObject>
{
    private final CRMReportManagerAPI crmManager;
    private final CRMViewManagerAPI viewManager;

    /**
     * Creates dashboard-visibility update validator dependencies.
     */
    private DashboardVisibilityUpdateValidator()
    {
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.viewManager = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final DashboardVisibilityUpdateValidator INSTANCE = new DashboardVisibilityUpdateValidator();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link DashboardVisibilityUpdateValidator} singleton
     */
    public static DashboardVisibilityUpdateValidator getInstance()
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
    public void validateRequest( final @NonNull Long orgId,
                                 final @NonNull Long userId,
                                 final @NonNull JsonObject jsonObject ) throws Exception
    {
        final Long dashboardId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );

        if( !viewManager.getCRMDashboardManager()
                        .exists( orgId, dashboardId ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                    "i18n.analytics.config.dashboard.invalid.id" );
        }

        final Visibility visibility = Visibility.get( jsonObject.getShort( CRMReportKeys.VISIBILITY_ID ) );

        if( Objects.isNull( visibility ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY,
                         "i18n.crm.analytics.visibility.invalid" );
        }

        final CRMViewConfigManager<DashboardVisibility> dashboardVisibilityManager = crmManager.getDashBoardVisibilityManager();
        final Criteria criteria = Criteria.eq( dashboardVisibilityManager.getTable()
                                                                         .getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ), dashboardId );
        final Collection<DashboardVisibility> dashboardVisibilities = dashboardVisibilityManager.getAll( orgId, criteria );

        if( Objects.nonNull( dashboardVisibilities ) && !dashboardVisibilities.isEmpty() )
        {
            final DashboardVisibility dashboardVisibility = dashboardVisibilities.stream()
                                                                                 .findAny()
                                                                                 .orElse(null);
            final Long justMeUserVisibility = dashboardVisibility.getModifiedBy();

            switch( visibility )
            {
                case JUST_ME ->
                {
                    if( !Objects.equals( justMeUserVisibility, userId ) )
                    {
                        throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                     "i18n.crm.analytics.visibility.permission.invalid" );
                    }
                }
                case SHARED_WITH ->
                {
                    if( jsonObject.containsKey( CRMDashboardKeys.EVERYONE )  )
                    {
                        final JsonObject everyoneJson = jsonObject.getJsonObject( CRMDashboardKeys.EVERYONE );

                        validateVisibilityPermission( everyoneJson.getShort( CRMDashboardKeys.ACCESS ) );
                    }

                    if( jsonObject.containsKey( CRMDashboardKeys.USERS ) )
                    {
                        validDashboardVisibilityPermission( jsonObject.getJsonArray( CRMDashboardKeys.USERS ) );
                    }

                    if( jsonObject.containsKey( CRMDashboardKeys.TEAMS ) )
                    {
                        validDashboardVisibilityPermission( jsonObject.getJsonArray( CRMDashboardKeys.TEAMS ) );
                    }

                    if( jsonObject.containsKey( CRMDashboardKeys.TERRITORIES ) )
                    {
                        validDashboardVisibilityPermission( jsonObject.getJsonArray( CRMDashboardKeys.TERRITORIES ) );
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Validates the provided visibility permission id to ensure it corresponds to a valid visibility permission
     * </p>
     *
     * @param visibilityPermissionId Represents the dashboard visibility permission id to validate
     */
    private void validateVisibilityPermission( final short visibilityPermissionId )
    {
        final VisibilityPermission visibilityPermission = VisibilityPermission.get( visibilityPermissionId );

        if( Objects.isNull( visibilityPermission ) )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Validates the dashboard visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param jsonArray Represents the dashboard visibility details
     */
    private void validDashboardVisibilityPermission( final JsonArray jsonArray )
    {
        final int totalSize = jsonArray.size();

        for(int index = 0; index < totalSize; index++ )
        {
            final JsonObject jsonObject = jsonArray.getJsonObject( index );
            final short visibilityPermissionId = jsonObject.getShort( CRMDashboardKeys.ACCESS );

            validateVisibilityPermission( visibilityPermissionId );
        }
    }
}
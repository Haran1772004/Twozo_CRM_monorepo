package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout trash configuration
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public class DashboardTrashValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private DashboardTrashValidator()
    {
        crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final DashboardTrashValidator DASHBOARD_TRASH_VALIDATOR = new DashboardTrashValidator();
    }

    public static DashboardTrashValidator getInstance()
    {
        return Instance.DASHBOARD_TRASH_VALIDATOR;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param request Represents the request data of type
     */
    @Override
    public void validateRequest( final @NonNull Long orgId, final @NonNull Long userId, final @NonNull JsonObject request ) throws Exception
    {
        final Long dashboardId = request.getLong( CRMReportKeys.DASHBOARD_ID );
        final boolean isExists = crmViewManager.getCRMDashboardManager()
                                               .exists( orgId, dashboardId );

        if( !isExists )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
        final CRMDashboardDetails dashboardDetails = crmViewManager.getCRMDashboardManager()
                                                                   .get( orgId, dashboardId );

        if( dashboardDetails.isMarkAsTrash() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_ALREADY_MOVED_TO_TRASH,
                                 "i18n.crm.analytics.dashboard.already.move.to.trash" );
        }
    }
}

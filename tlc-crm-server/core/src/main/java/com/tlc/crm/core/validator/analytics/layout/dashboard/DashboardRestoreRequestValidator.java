package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash.CRMDashboardTrash;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDTRASH;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.sql.common.api.dml.Criteria;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout restore configuration
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public class DashboardRestoreRequestValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManagerAPI;

    private DashboardRestoreRequestValidator()
    {
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final DashboardRestoreRequestValidator DASHBOARD_RESTORE_VALIDATOR = new DashboardRestoreRequestValidator();
    }

    public static DashboardRestoreRequestValidator getInstance()
    {
        return Instance.DASHBOARD_RESTORE_VALIDATOR;
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
        final CRMViewConfigManager<CRMDashboardTrash> dashboardTrashManager = crmViewManagerAPI.getCRMDashboardTrashManager();
        final Long dashboardId = request.getLong( CRMReportKeys.DASHBOARD_ID );
        final boolean dashboardExists = crmViewManagerAPI.getCRMDashboardManager()
                                                         .exists( orgId,  dashboardId );

        if( !dashboardExists )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
        final Collection<CRMDashboardTrash> crmDashboardTrashList = dashboardTrashManager.getAll( orgId,
                                                                                                  Criteria.eq( dashboardTrashManager.getTable()
                                                                                                                                    .getColumn( CRMDASHBOARDTRASH.DASHBOARD_ID ),
                                                                                                               dashboardId ) );

        if( crmDashboardTrashList.isEmpty() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NOT_FOUND,
                                 "i18n.crm.analytics.dashboard.has.not.been.trashed" );
        }
    }
}

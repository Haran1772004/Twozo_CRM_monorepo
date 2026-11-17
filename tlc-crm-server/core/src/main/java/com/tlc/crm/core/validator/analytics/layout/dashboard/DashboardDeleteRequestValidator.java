package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout delete configuration
 * </p>
 *
 * @author Pandiarajkumar G
 * @version 1.0
 */
public class DashboardDeleteRequestValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private static class Instance
    {
        private static final DashboardDeleteRequestValidator INSTANCE = new DashboardDeleteRequestValidator();
    }

    private DashboardDeleteRequestValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    public static DashboardDeleteRequestValidator INSTANCE()
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
        final boolean dashboardExists = crmViewManager.getCRMDashboardManager()
                                                      .exists( orgId, jsonObject.getLong( CRMReportKeys.DASHBOARD_ID ) );

        if( !dashboardExists )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
    }
}

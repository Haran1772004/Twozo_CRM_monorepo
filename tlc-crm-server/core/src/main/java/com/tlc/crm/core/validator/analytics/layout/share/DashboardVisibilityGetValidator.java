package com.tlc.crm.core.validator.analytics.layout.share;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to validate the dashboard share visibility configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class DashboardVisibilityGetValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private DashboardVisibilityGetValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final DashboardVisibilityGetValidator INSTANCE = new DashboardVisibilityGetValidator();
    }

    public static DashboardVisibilityGetValidator getInstance()
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
        try
        {
            final boolean dashboardExists = crmViewManager.getCRMDashboardManager()
                                                          .exists( orgId, jsonObject.getLong( CRMReportKeys.DASHBOARD_ID ) );

            if( !dashboardExists )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                     "i18n.analytics.config.dashboard.invalid.id" );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
    }
}


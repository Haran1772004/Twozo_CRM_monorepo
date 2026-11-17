package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.crm.common.util.NumberUtils;

import lombok.NonNull;

import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout clone configuration
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 * @see RequestValidator
 */
public class DashboardCloneValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;
    private final int dashboardNameLimit;
    private final int dashboardDescriptionLimit;

    private static class Instance
    {
        private static final DashboardCloneValidator INSTANCE = new DashboardCloneValidator();
    }

    private DashboardCloneValidator()
    {
        final Configurations configuration = ConfHolder.getInstance()
                                                       .getProperties( CRMReportKeys.ANALYTICS )
                                                       .getNestedConfigs( CRMReportKeys.DASHBOARD );
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
        this.dashboardNameLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.NAME_LIMIT) )
                                             .intValue();
        this.dashboardDescriptionLimit = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.DESCRIPTION_LIMIT) )
                                                    .intValue();
    }

    public static DashboardCloneValidator INSTANCE()
    {
        return DashboardCloneValidator.Instance.INSTANCE;
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
        final String dashboardName = jsonObject.getString( CRMDashboardKeys.DASHBOARD_NAME );

        if ( Objects.isNull( dashboardName ) || dashboardName.isBlank() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_SHOULD_NOT_BE_EMPTY,
                         "i18n.analytics.config.dashboard.invalid.name" );
        }

        if ( dashboardNameLimit < dashboardName.length() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NAME_TOO_LONG,
                         "i18n.analytics.config.dashboard.name.limit.exceeded");
        }

        if ( jsonObject.containsKey( CRMDashboardKeys.DESCRIPTION ) )
        {
            final String description = jsonObject.getString( CRMDashboardKeys.DESCRIPTION );

            if ( Objects.nonNull( description ) && dashboardDescriptionLimit < description.length() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_DESCRIPTION_TOO_LONG,
                             "i18n.analytics.config.dashboard.description.limit.exceeded"  );
            }
        }
    }
}

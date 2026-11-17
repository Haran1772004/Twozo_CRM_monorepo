package com.tlc.crm.core.validator.analytics.layout.dashboard;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMCHILDVIEWDETAILS;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way to validate the dashboard layout move configuration
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class DashboardTransferValidator implements RequestValidator<JsonObject>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DashboardTransferValidator.class );
    private static final int DASHBOARD_REQUIRED_LIMIT = 2;

    private final CRMViewManagerAPI crmViewManagerAPI;
    private final int dashboardMaxLimit;

    private DashboardTransferValidator()
    {
        this.crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        this.dashboardMaxLimit = NumberUtils.parseNumber( ConfHolder.getInstance()
                                                                    .getProperties( CRMReportKeys.ANALYTICS )
                                                                    .getNestedConfigs( CRMReportKeys.DASHBOARD )
                                                                    .getConfig( CRMReportKeys.VIEW_LIMIT) )
                                            .intValue();
    }

    private static final class Instance
    {
        private static final DashboardTransferValidator DASHBOARD_TRANSFER_VALIDATOR = new DashboardTransferValidator();
    }

    public static DashboardTransferValidator getInstance()
    {
        return Instance.DASHBOARD_TRANSFER_VALIDATOR;
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
        final Long sourceDashboardId = jsonObject.getLong( CRMDashboardKeys.SOURCE_DASHBOARD_ID );
        final Long targetDashboardId = jsonObject.getLong( CRMDashboardKeys.TARGET_DASHBOARD_ID );
        final Long viewId = jsonObject.getLong( CRMReportKeys.VIEW_ID );
        final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager = crmViewManagerAPI.getCRMDashboardManager();
        final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManagerAPI.getCRMChildViewManager();
        final Table childViewTable = crmChildViewManager.getTable();
        final Collection<CRMDashboardDetails> crmDashboardDetails = crmDashboardManager.get( orgId, List.of( sourceDashboardId,
                                                                                                             targetDashboardId ) );


        if( crmDashboardDetails.isEmpty() || crmDashboardDetails.size() < DASHBOARD_REQUIRED_LIMIT )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_NOT_FOUND,
                                 "i18n.analytics.dashboard.not.found" );
        }

        final Collection<CRMChildViewDetails> sourceChildViewDetails = crmChildViewManager.getAll( orgId, Criteria.eq( childViewTable.getColumn( CRMCHILDVIEWDETAILS.CRM_DASHBOARD_ID ),
                                                                                                                                           sourceDashboardId ) );
        LOGGER.info( "CRM Analytics: Fetched the source dashboard parent view for orgId:{}, dashboard id:{}", orgId, sourceDashboardId );
        sourceChildViewDetails.stream()
                              .filter( crmChildView -> Objects.equals( viewId, crmChildView.id() ) )
                              .findFirst()
                              .orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                                                 "i18n.analytics.config.view.invalid.id" ) );

        final Collection<CRMChildViewDetails> targetChildViewDetails = crmChildViewManager.getAll( orgId, Criteria.eq( childViewTable.getColumn( CRMCHILDVIEWDETAILS.CRM_DASHBOARD_ID ),
                                                                                                                                                 targetDashboardId ) );

        LOGGER.info( "CRM Analytics: Fetched the target dashboard parent view for orgId:{}, dashboard id:{}", orgId, targetDashboardId );

        if( dashboardMaxLimit == targetChildViewDetails.size() )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.DASHBOARD_VIEW_LIMIT_EXCEEDED,
                                 "i18n.crm.analytics.dashboard.widget.maximum.reached" );
        }
    }
}

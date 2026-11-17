package com.tlc.crm.core.validator.analytics.layout.view;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMCHILDVIEWDETAILS;
import com.tlc.crm.core.validator.analytics.RequestValidator;

import com.tlc.sql.common.api.dml.Criteria;
import lombok.NonNull;

import java.util.Collection;
import java.util.stream.Collectors;


/**
 * <p>
 * Provides an abstract way to validate the view get configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see RequestValidator
 */
class ViewGetValidator implements RequestValidator<JsonObject>
{
    private final CRMViewManagerAPI crmViewManager;

    private ViewGetValidator()
    {
        this.crmViewManager = CRMViewManagerAPI.INSTANCE();
    }

    private static class Instance
    {
        private static final ViewGetValidator INSTANCE = new ViewGetValidator();
    }

    public static ViewGetValidator INSTANCE()
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
        final Long dashId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );
        final Long viewId = jsonObject.getLong( CRMReportKeys.VIEW_ID );
        final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager = crmViewManager.getCRMDashboardManager();
        final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManager.getCRMChildViewManager();

        if( crmDashboardManager.exists( orgId, dashId ) )
        {
            final Criteria criteria = Criteria.eq( crmChildViewManager.getTable().getColumn( CRMCHILDVIEWDETAILS.CRM_DASHBOARD_ID ), dashId );
            final Collection<CRMChildViewDetails> childViewDetails = crmChildViewManager.getAll( orgId, criteria );
            final Collection<Long> childViewIds = childViewDetails.stream()
                                                                  .map( CRMChildViewDetails::id )
                                                                  .collect( Collectors.toSet() );
            if( childViewIds.isEmpty() || !childViewIds.contains( viewId ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                     "i18n.analytics.config.view.invalid.id" );
            }
        }
        else
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
    }
}

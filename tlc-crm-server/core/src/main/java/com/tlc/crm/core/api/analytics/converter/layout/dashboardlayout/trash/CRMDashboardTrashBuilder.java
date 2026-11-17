package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.trash;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash.CRMDashboardTrash;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.interval.DateInterval;

import lombok.NonNull;

import java.time.Instant;

/**
 * <p>
 * Responsible for building various components of {@link CRMDashboardDetails}
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public class CRMDashboardTrashBuilder
{
    private final int trashExpiryDays;

    private CRMDashboardTrashBuilder()
    {
        final Configurations configuration = ConfHolder.getInstance()
                                                       .getProperties( CRMReportKeys.ANALYTICS )
                                                       .getNestedConfigs( CRMReportKeys.DASHBOARD );
        this.trashExpiryDays = NumberUtils.parseNumber( configuration.getConfig( CRMReportKeys.TRASH_EXPIRY_DAYS ) )
                                          .intValue();
    }

    private static class Instance
    {
        private static final CRMDashboardTrashBuilder CRM_DASHBOARD_TRASH_BUILDER = new CRMDashboardTrashBuilder();
    }

    public static CRMDashboardTrashBuilder getInstance()
    {
        return Instance.CRM_DASHBOARD_TRASH_BUILDER;
    }

    /**
     * <p>
     * Builds the {@link CRMDashboardDetails} based on the existing dashboard details
     * </p>
     *
     * @param isTrashed           A flag indicating whether the dashboard is currently in the trash.
     * @param orgId               Represents an organisation id
     * @param userId              Represents the user id
     * @param crmDashboardDetails Represents the {@link CRMDashboardDetails}
     * @return The {@link CRMDashboardDetails}
     */
    public CRMDashboardDetails buildDashboardDetails( final boolean isTrashed,
                                                      final @NonNull Long orgId,
                                                      final @NonNull Long userId,
                                                      final @NonNull CRMDashboardDetails crmDashboardDetails )
    {

        return CRMDashboardDetails.builder()
                                  .reportsDashboardId( crmDashboardDetails.getReportsDashboardId() )
                                  .id( crmDashboardDetails.id() )
                                  .crmChildViews( crmDashboardDetails.getCrmChildViews() )
                                  .createdBy( crmDashboardDetails.getCreatedBy() )
                                  .createdTime( crmDashboardDetails.getCreatedTime() )
                                  .description( crmDashboardDetails.getDescription() )
                                  .isCurated( crmDashboardDetails.isCurated() )
                                  .markAsTrash( isTrashed )
                                  .modifiedBy( userId )
                                  .modifiedTime( Instant.now()
                                                        .toEpochMilli() )
                                  .orgId( orgId )
                                  .name( crmDashboardDetails.getName() )
                                  .crmDashboardTrash( isTrashed ? buildDashboardTrash( crmDashboardDetails.id() )
                                                                : null )
                                  .build();
    }

    /**
     * <p>
     * Builds the {@link CRMDashboardTrash} detail based on the given dashboard id
     * </p>
     *
     * @param dashboardId Represents the dashboard id
     * @return The {@link CRMDashboardTrash} details
     */
    private CRMDashboardTrash buildDashboardTrash( final Long dashboardId )
    {
        return CRMDashboardTrash.builder()
                                .trashedAt( TemporalServiceProvider.get()
                                                                   .plus( trashExpiryDays, DateInterval.DAYS ) )
                                .dashboardId( dashboardId )
                                .build();
    }
}

package com.tlc.crm.core.action.analytics.list;

import com.tlc.analytics.sql.resource.ANADASHBOARDLAYOUT;

import com.tlc.crm.common.list.JoinLoader;

import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;
import com.tlc.crm.core.sql.resource.CRMFAVOURITEVIEW;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.List;

/**
 * Handles the HTTP requests that are relevant to fav reports list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/fav/list" )
public class FavouriteList extends CrmAnalyticsListAction
{

    /**
     * {@inheritDoc}
     *
     * @param preAction {@link JoinLoader}
     */
    @Override
    protected void loadJoins( final @NonNull JoinLoader preAction )
    {
        final Table analyticsDashboardTable = analyticsServiceAPI.getDashboardLayout()
                                                                 .getTable();
        final Column viewTablePKColumn = analyticsServiceAPI.getViewDetailsManager()
                                                            .getTable()
                                                            .getPKColumn();
        final Table crmDashboardTable = crmViewMangerAPI.getCRMDashboardManager()
                                                        .getTable();

        preAction.addJoinClause( List.of( new JoinClause( crmDashboardTable.getColumn( CRMDASHBOARDDETAILS.ANA_DASHBOARD_ID ),
                                                          analyticsDashboardTable.getPKColumn() ),
                                          new JoinClause( analyticsDashboardTable.getColumn( ANADASHBOARDLAYOUT.VIEW_ID ),
                                                          viewTablePKColumn ),
                                          new JoinClause( crmDashboardTable.getPKColumn(),
                                                          crmViewMangerAPI.getCRMFavouriteManager()
                                                                          .getTable()
                                                                          .getColumn( CRMFAVOURITEVIEW.CRM_DASHBOARD_ID ) ) ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param userId      Represents the user ID
     * @param whereClause The initial {@link WhereClause} to which additional conditions are appended
     * @return The updated {@link WhereClause} with additional conditions
     */
    @Override
    protected WhereClause append( final @NonNull Long userId, final @NonNull WhereClause whereClause )
    {
        return whereClause.and( Criteria.eq( crmViewMangerAPI.getCRMFavouriteManager()
                                                             .getTable()
                                                             .getColumn( CRMFAVOURITEVIEW.USER_ID ),
                                             userId ) );
    }
}

package com.tlc.crm.core.action.analytics.list;

import com.tlc.crm.common.list.JoinLoader;

import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.List;

/**
 * Handles the HTTP requests that are relevant to private reports list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/owned/list" )
public class OwnedList extends CrmAnalyticsListAction
{
    /**
     * {@inheritDoc}
     *
     * @param preAction {@link JoinLoader}
     */
    @Override
    protected void loadJoins( final @NonNull JoinLoader preAction )
    {

        final Table dashboardVisibility = getDashboardVisibilityTable();

        preAction.addJoinClause( List.of( new JoinClause( getCrmDashboardTable().getColumn( CRMDASHBOARDDETAILS.ID ),
                                                          dashboardVisibility.getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ) ) ) );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns only dashboards created by the current user that are not trashed.</p>
     *
     * @return The additional {@link WhereClause}
     */
    @Override
    protected WhereClause getAdditionalWhereClause( final @NonNull Long userId )
    {
        final Table dashboardVisibility = getDashboardVisibilityTable();

        return new WhereClause( Criteria.eq( getCrmDashboardTable().getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ), false ) )
                           .and( Criteria.eq( dashboardVisibility.getColumn( OMCANADASHBOARDVISBILITY.CREATED_BY ), userId ) );
    }
}

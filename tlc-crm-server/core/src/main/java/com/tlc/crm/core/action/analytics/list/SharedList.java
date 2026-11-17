package com.tlc.crm.core.action.analytics.list;

import com.tlc.crm.common.list.JoinLoader;

import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;
import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Handles the HTTP requests that are relevant to shared reports list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/shared/list" )
public class SharedList extends CrmAnalyticsListAction
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
     * @return The additional {@link WhereClause}
     */
    @Override
    protected WhereClause getAdditionalWhereClause( final @NonNull Long userId )
    {
        final Table dashboardVisibility = getDashboardVisibilityTable();
        final Column dashboardVisibilityPkColumn = dashboardVisibility.getPKColumn();
        final WhereClause whereClause = new WhereClause( Criteria.in( dashboardVisibilityPkColumn, getUserVisibilityQuery( userId ) ) )
                                                    .or( Criteria.in( dashboardVisibilityPkColumn, getEveryOneVisibilityQuery( userId ) ) );

        return append( userId, whereClause );
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
        final Long orgId = LoggedInUserInfo.getUser()
                                           .orgId();
        final Table dashboardVisibility = getDashboardVisibilityTable();
        final Column dashboardVisibilityPkColumn = dashboardVisibility.getPKColumn();
        final SelectQuery teamVisibilityQuery = getTeamVisibilityQuery( orgId, userId );
        final SelectQuery territoryVisibilityQuery = getTerritoryVisibilityQuery( orgId, userId );
        final WhereClause teamWhereClause = Objects.nonNull( teamVisibilityQuery ) ? whereClause.or( Criteria.in( dashboardVisibilityPkColumn, teamVisibilityQuery ) )
                                                                                   : whereClause;
        return Objects.nonNull( territoryVisibilityQuery ) ? teamWhereClause.or( Criteria.in( dashboardVisibilityPkColumn, territoryVisibilityQuery ) )
                                                           : teamWhereClause;
    }
}

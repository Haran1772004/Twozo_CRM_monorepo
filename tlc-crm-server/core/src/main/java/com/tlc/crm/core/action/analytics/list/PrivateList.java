package com.tlc.crm.core.action.analytics.list;

import com.tlc.crm.common.list.JoinLoader;
import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.api.analytics.entity.share.Visibility;
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
@WebAction( path = "/crm/analytics/private/list" )
public class PrivateList extends CrmAnalyticsListAction
{

    /**
     * {@inheritDoc}
     *
     * @param preAction {@link JoinLoader}
     */
    @Override
    protected void loadJoins( final @NonNull JoinLoader preAction )
    {
        preAction.addJoinClause( List.of( new JoinClause( getCrmDashboardTable().getColumn( CRMDASHBOARDDETAILS.ID ),
                                                          getDashboardVisibilityTable().getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ) ) ) );
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
        final Table dashboardVisibilityTable = getDashboardVisibilityTable();

        return whereClause.and( new WhereClause( Criteria.eq( dashboardVisibilityTable.getColumn( OMCANADASHBOARDVISBILITY.CREATED_BY ),
                                                              userId ) )
                                           .and( Criteria.eq( dashboardVisibilityTable.getColumn( OMCANADASHBOARDVISBILITY.VISIBILITY_ID ),
                                                              Visibility.JUST_ME.getId() ) ) );
    }
}

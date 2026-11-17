package com.tlc.crm.core.action.analytics.list;

import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * Handles the HTTP requests that are relevant to curated reports list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/curated/list" )
public class CuratedList extends CrmAnalyticsListAction
{

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
        final Table crmDashboardTable = getCrmDashboardTable();

        return whereClause.and( Criteria.eq( crmDashboardTable.getColumn( CRMDASHBOARDDETAILS.CREATED_BY ), userId ) )
                          .and( Criteria.eq( crmDashboardTable.getColumn( CRMDASHBOARDDETAILS.IS_CURATED ), true ) );
    }
}

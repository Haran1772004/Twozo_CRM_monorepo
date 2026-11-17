package com.tlc.crm.core.action.analytics.list;

import com.tlc.crm.core.action.analytics.CrmAnalyticsListAction;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;

import com.tlc.crm.entity.list.model.Provider;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * Handles the HTTP requests that are relevant to reports trash list action
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/trash/list" )
public class TrashList extends CrmAnalyticsListAction
{

    /**
     * {@inheritDoc}
     *
     * @return The provider name
     */
    @Override
    protected String getProviderName()
    {
        return Provider.REPORTS_TRASH_LIST.name();
    }

    /**
     * {@inheritDoc}
     *
     * @return The additional {@link WhereClause}
     */
    @Override
    protected WhereClause getAdditionalWhereClause( final @NonNull Long userId )
    {
        final Table crmDashboardTable = getCrmDashboardTable();

        return new WhereClause( Criteria.eq( crmDashboardTable.getColumn( CRMDASHBOARDDETAILS.CREATED_BY ), userId ) )
                          .and( Criteria.eq( crmDashboardTable.getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ), true ) );
    }
}

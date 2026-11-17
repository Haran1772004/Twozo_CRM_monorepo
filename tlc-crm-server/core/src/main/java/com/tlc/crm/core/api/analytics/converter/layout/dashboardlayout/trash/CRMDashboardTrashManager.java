package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.trash;

import com.tlc.crm.core.api.analytics.integ.managers.CRMAbstractReportsManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.trash.CRMDashboardTrash;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDTRASH;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to manage the {@link CRMDashboardTrash} db related actions
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
@NoArgsConstructor( access =  AccessLevel.PRIVATE )
public class CRMDashboardTrashManager extends CRMAbstractReportsManager<CRMDashboardTrash>
{
    private static class Instance
    {
        private static final CRMDashboardTrashManager CRM_DASHBOARD_TRASH_MANAGER = new CRMDashboardTrashManager();
    }

    public static CRMDashboardTrashManager getInstance()
    {
        return Instance.CRM_DASHBOARD_TRASH_MANAGER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Table getTable()
    {
        return Table.get( CRMDASHBOARDTRASH.TABLE );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected @NonNull Class<CRMDashboardTrash> getModelClass()
    {
        return CRMDashboardTrash.class;
    }

    /**
     * <p>
     * Deletes the dashboards which are trashed after 180 days
     * </p>
     */
    public void deleteDashboard()
    {
        final AdminDataStore adminDataStore = DataStoreProvider.getAdminDataStore();
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get( table );

        selectQuery.addSelectClause( table );
        selectQuery.setWhereClause( new WhereClause( Criteria.le( table.getColumn( CRMDASHBOARDTRASH.TRASHED_AT ),
                                                                  Instant.now()
                                                                         .toEpochMilli() ) ) );
        final DataContainer dataContainer = adminDataStore.get( selectQuery );

        final Collection<Long> dashboardIds = dataContainer.getRows( table )
                                                           .map( row -> ( Long ) row.get( CRMDASHBOARDTRASH.DASHBOARD_ID ) )
                                                           .toList();

        if( !dashboardIds.isEmpty() )
        {
            adminDataStore.delete( CRMViewManagerAPI.INSTANCE()
                                                    .getCRMDashboardManager()
                                                    .getTable(), dashboardIds );
        }
    }
}

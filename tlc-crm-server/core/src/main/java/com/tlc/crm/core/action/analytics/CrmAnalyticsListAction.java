package com.tlc.crm.core.action.analytics;

import com.tlc.crm.common.action.list.SecureListAction;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;

import com.tlc.crm.core.api.analytics.entity.share.Visibility;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;
import com.tlc.crm.core.sql.resource.MCANADEVERYONEVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTEAMVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTERRITORYVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADUSERVISBILITY;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;
import com.tlc.crm.entity.list.model.Provider;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.NonNull;

import java.util.Collection;
import java.util.Objects;


/**
 * <p>
 * {@link CrmAnalyticsListAction} is an abstract class that serves as a base for implementing {@link SecureListAction}
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public abstract class CrmAnalyticsListAction extends SecureListAction
{
    protected final AnalyticsServiceAPI analyticsServiceAPI;
    protected final CRMViewManagerAPI crmViewMangerAPI;
    protected final CRMReportManagerAPI crmManager;
    protected final TeamManager teamManager;
    protected final TerritoryManager territoryManager;

    /**
     * Creates analytics list-action dependencies.
     */
    protected CrmAnalyticsListAction()
    {
        this.analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        this.crmViewMangerAPI = CRMViewManagerAPI.INSTANCE();
        this.crmManager = CRMReportManagerAPI.INSTANCE();
        this.teamManager = TeamManager.getInstance();
        this.territoryManager = TerritoryManager.getInstance();
    }

    /**
     * <p>
     * Retrieves the table representing crm dashboard data
     * </p>
     *
     * @return the {@link Table} that contains the crm dashboard details
     */
    protected Table getCrmDashboardTable()
    {
        return crmViewMangerAPI.getCRMDashboardManager()
                               .getTable();
    }

    /**
     * <p>
     * Retrieves the table representing dashboard visibility data
     * </p>
     *
     * @return the {@link Table} that contains the dashboard visibility details
     */
    protected Table getDashboardVisibilityTable()
    {
        return crmManager.getDashBoardVisibilityManager()
                         .getTable();
    }

    /**
     * <p>
     * Retrieves the table representing dashboard user visibility data
     * </p>
     *
     * @return the {@link Table} that contains the dashboard user visibility details
     */
    protected Table getDashboardUserVisibilityTable()
    {
        return crmManager.getDashBoardUserVisibilityManager()
                         .getTable();
    }

    /**
     * <p>
     * Retrieves the table representing dashboard team visibility data
     * </p>
     *
     * @return the {@link Table} that contains the dashboard team visibility details
     */
    protected Table getDashboardTeamVisibilityTable()
    {
        return crmManager.getDashBoardTeamVisibilityManager()
                         .getTable();
    }

    /**
     * <p>
     * Retrieves the table representing dashboard territory visibility data
     * </p>
     *
     * @return the {@link Table} that contains the dashboard territory visibility details
     */
    protected Table getDashboardTerritoryVisibilityTable()
    {
        return crmManager.getDashBoardTerritoryVisibilityManager()
                         .getTable();
    }

    /**
     * <p>
     * Retrieves the table representing dashboard everyone visibility data
     * </p>
     *
     * @return the {@link Table} that contains the dashboard everyone visibility details
     */
    protected Table getDashboardEveryoneVisibilityTable()
    {
        return crmManager.getDashBoardEveryoneVisibilityManager()
                         .getTable();
    }

    /**
     * {@inheritDoc}
     *
     * @return The provider name
     */
    @Override
    protected String getProviderName()
    {
        return Provider.REPORTS_LIST.name();
    }

    /**
     * {@inheritDoc}
     *
     * @return The additional {@link WhereClause}
     */
    @Override
    protected WhereClause getAdditionalWhereClause( final @NonNull Long userId )
    {
        return append( userId, new WhereClause( Criteria.eq( crmViewMangerAPI.getCRMDashboardManager()
                                                                             .getTable()
                                                                             .getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ),
                                                             false ) ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param userId      Represents the user ID
     * @param whereClause The initial {@link WhereClause} to which additional conditions are appended
     * @return The updated {@link WhereClause} with additional conditions
     */
    protected WhereClause append( final @NonNull Long userId, final @NonNull WhereClause whereClause )
    {
        return whereClause;
    }

    /**
     * <p>
     * Constructs the {@link SelectQuery} that retrieves dashboard shared visibility records
     * </p>
     *
     * @param userId Represents the user id whose dashboard visibility records are being retrieved
     * @return A {@link SelectQuery} configured to fetch dashboard visibility records for the specified user
     */
    private SelectQuery getDashboardSharedVisibilityQuery( final Long userId )
    {
        final Table dashboardVisibilityTable = getDashboardVisibilityTable();
        final SelectQuery selectQuery = SelectQuery.get( dashboardVisibilityTable );

        selectQuery.addSelectClause( dashboardVisibilityTable.getPKColumn() );
        selectQuery.setWhereClause( new WhereClause( Criteria.eq( getCrmDashboardTable().getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ), false ) )
                                               .and( Criteria.notEq( dashboardVisibilityTable.getColumn( OMCANADASHBOARDVISBILITY.CREATED_BY ), userId  ) )
                                               .and( Criteria.eq( dashboardVisibilityTable.getColumn( OMCANADASHBOARDVISBILITY.VISIBILITY_ID ), Visibility.SHARED_WITH.getId() ) ) );

        return selectQuery;
    }

    /**
     * <p>
     * Constructs the {@link SelectQuery} that retrieves dashboard shared user visibility records
     * </p>
     *
     * @param userId Represents the user id whose dashboard visibility records are being retrieved
     * @return A {@link SelectQuery} configured to fetch dashboard visibility records for the specified user
     */
    protected SelectQuery getUserVisibilityQuery( final Long userId )
    {
        final Table dashboardUserVisibility = getDashboardUserVisibilityTable();
        final Column dashbaordVisibilityIdColumn = dashboardUserVisibility.getColumn( MCANADUSERVISBILITY.DASHBOARD_VISIBILITY_ID );
        final SelectQuery selectQuery = SelectQuery.get( dashboardUserVisibility );

        selectQuery.addSelectClause( dashbaordVisibilityIdColumn );
        selectQuery.setWhereClause( new WhereClause( Criteria.eq( dashboardUserVisibility.getColumn( MCANADUSERVISBILITY.USER_ID ), userId ) )
                                               .and( Criteria.in( dashbaordVisibilityIdColumn, getDashboardSharedVisibilityQuery( userId ) ) ) );

        return selectQuery;
    }

    /**
     * <p>
     * Constructs the {@link SelectQuery} that retrieves dashboard shared team visibility records
     * </p>
     *
     * @param userId Represents the user id whose dashboard visibility records are being retrieved
     * @return A {@link SelectQuery} configured to fetch dashboard visibility records for the specified user
     */
    protected SelectQuery getTeamVisibilityQuery( final Long orgId, final Long userId )
    {
        final Collection<Long> userAccessibleTeamIds = teamManager.getUserTeamIds( orgId, userId );

        if( Objects.nonNull( userAccessibleTeamIds ) && !userAccessibleTeamIds.isEmpty() )
        {
            final Table dashboardTeamVisibilityTable = getDashboardTeamVisibilityTable();
            final Column dashbaordVisibilityIdColumn = dashboardTeamVisibilityTable.getColumn( MCANADTEAMVISBILITY.DASHBOARD_VISIBILITY_ID );
            final SelectQuery selectQuery = SelectQuery.get( dashboardTeamVisibilityTable );

            selectQuery.addSelectClause( dashbaordVisibilityIdColumn );
            selectQuery.setWhereClause( new WhereClause( Criteria.in( dashbaordVisibilityIdColumn, getDashboardSharedVisibilityQuery( userId ) ) )
                                                   .and( Criteria.in( dashboardTeamVisibilityTable.getColumn( MCANADTEAMVISBILITY.TEAM_ID ), userAccessibleTeamIds ) ) );

            return selectQuery;
        }

        return null;
    }

    /**
     * <p>
     * Constructs the {@link SelectQuery} that retrieves dashboard shared territory visibility records
     * </p>
     *
     * @param userId Represents the user id whose dashboard visibility records are being retrieved
     * @return A {@link SelectQuery} configured to fetch dashboard visibility records for the specified user
     */
    protected SelectQuery getTerritoryVisibilityQuery( final Long orgId, final Long userId )
    {
        final Collection<Long> userAccessibleTerritoryIds = territoryManager.getUserTerritoryIds( orgId, userId );

        if( Objects.nonNull( userAccessibleTerritoryIds ) && !userAccessibleTerritoryIds.isEmpty() )
        {
            final Table dashboardTerritoryVisibilityTable = getDashboardTerritoryVisibilityTable();
            final Column dashbaordVisibilityIdColumn = dashboardTerritoryVisibilityTable.getColumn( MCANADTERRITORYVISBILITY.DASHBOARD_VISIBILITY_ID );
            final SelectQuery selectQuery = SelectQuery.get( dashboardTerritoryVisibilityTable );

            selectQuery.addSelectClause( dashbaordVisibilityIdColumn );
            selectQuery.setWhereClause( new WhereClause( Criteria.in( dashbaordVisibilityIdColumn, getDashboardSharedVisibilityQuery( userId ) ) )
                                                   .and( Criteria.in( dashboardTerritoryVisibilityTable.getColumn( MCANADTERRITORYVISBILITY.TERRITORY_ID ), userAccessibleTerritoryIds ) ) );

            return selectQuery;
        }

        return null;
    }

    /**
     * <p>
     * Constructs the {@link SelectQuery} that retrieves dashboard shared everyone visibility records
     * </p>
     *
     * @param userId Represents the user id whose dashboard visibility records are being retrieved
     * @return A {@link SelectQuery} configured to fetch dashboard visibility records for the specified user
     */
    protected SelectQuery getEveryOneVisibilityQuery( final Long userId )
    {
        final Table dashboardEveryoneVisibility = getDashboardEveryoneVisibilityTable();
        final Column dashbaordVisibilityIdColumn = dashboardEveryoneVisibility.getColumn( MCANADEVERYONEVISBILITY.DASHBOARD_VISIBILITY_ID );
        final SelectQuery selectQuery = SelectQuery.get( dashboardEveryoneVisibility );

        selectQuery.addSelectClause( dashbaordVisibilityIdColumn );
        selectQuery.setWhereClause( new WhereClause( Criteria.in( dashbaordVisibilityIdColumn, getDashboardSharedVisibilityQuery( userId ) ) ) );

        return selectQuery;
    }
}

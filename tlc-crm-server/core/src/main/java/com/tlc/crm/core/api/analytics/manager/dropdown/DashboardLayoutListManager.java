package com.tlc.crm.core.api.analytics.manager.dropdown;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.core.api.analytics.entity.share.DashboardEveryoneVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTeamVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTerritoryVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardUserVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardVisibility;
import com.tlc.crm.core.api.analytics.entity.share.Visibility;
import com.tlc.crm.core.api.analytics.entity.share.VisibilityPermission;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.sql.resource.CRMDASHBOARDDETAILS;
import com.tlc.crm.core.sql.resource.MCANADEVERYONEVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTEAMVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTERRITORYVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADUSERVISBILITY;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;

import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages the {@link DashboardLayout} related to DB actions
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class DashboardLayoutListManager
{
    private final CRMReportManagerAPI crmReportManagerAPI;
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMViewConfigManager<DashboardVisibility> dashBoardVisibilityManager;
    private final CRMViewConfigManager<DashboardEveryoneVisibility> dashboardEveryoneVisibilityManager;
    private final CRMViewConfigManager<DashboardTerritoryVisibility> dashboardTerritoryVisibilityManager;
    private final CRMViewConfigManager<DashboardTeamVisibility> dashboardTeamVisibilityManager;
    private final CRMViewConfigManager<DashboardUserVisibility> dashboardUserVisibilityManager;

    private DashboardLayoutListManager()
    {
        this.crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        this.crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        this.dashBoardVisibilityManager = crmReportManagerAPI.getDashBoardVisibilityManager();
        this.dashboardEveryoneVisibilityManager = crmReportManagerAPI.getDashBoardEveryoneVisibilityManager();
        this.dashboardTerritoryVisibilityManager = crmReportManagerAPI.getDashBoardTerritoryVisibilityManager();
        this.dashboardTeamVisibilityManager = crmReportManagerAPI.getDashBoardTeamVisibilityManager();
        this.dashboardUserVisibilityManager = crmReportManagerAPI.getDashBoardUserVisibilityManager();
    }

    private static class Instance
    {
        private static final DashboardLayoutListManager DASHBOARD_LAYOUT_MANAGER = new DashboardLayoutListManager();
    }

    public static DashboardLayoutListManager getInstance()
    {
        return Instance.DASHBOARD_LAYOUT_MANAGER;
    }

    /**
     * <p>
     * Gets the collection of {@link DashboardLayout} details based on the given criteria
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param userId        Represents the user id
     * @param dropdownLimit Represents the {@link DropdownLimit}
     * @param criteria      Represents the {@link Criteria}
     * @return The collection of {@link DashboardLayout}
     */
    public Collection<CRMDashboardDetails> getAll( final @NonNull Long orgId,
                                                   final @NonNull Long userId,
                                                   final @NonNull DropdownLimit dropdownLimit,
                                                   final @NonNull DropdownCriteria criteria )
    {
        final int start = dropdownLimit.start();
        final int limit = dropdownLimit.limit();
        final Collection<CRMDashboardDetails> dashboardDetails = getDashboardLayoutList( orgId,
                                                                                         userId,
                                                                                         start,
                                                                                         limit,
                                                                                         criteria,
                                                                                         Collections.emptyList() );
        final int dashboardDetailsSize = dashboardDetails.size();

        if( dashboardDetailsSize < limit )
        {
            final Collection<Long> existingDashboardIds = dashboardDetails.stream()
                                                                          .map( CRMDashboardDetails::id )
                                                                          .toList();
            final Table visibilityTable = dashBoardVisibilityManager.getTable();
            final Table dashboardTable = crmViewManagerAPI.getCRMDashboardManager().getTable();
            final Collection<Long> dashboardIds = new HashSet<>();
            final Map<Visibility, Map<Long, Long>> visibilityMap = new EnumMap<>( Visibility.class );
            final SelectQuery selectQuery = SelectQuery.get( visibilityTable );

            selectQuery.addSelectClause( visibilityTable );
            selectQuery.addJoinClause( new JoinClause( visibilityTable.getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ),
                                                       dashboardTable.getPKColumn() ) );

            final Criteria trashFilter = Criteria.eq( dashboardTable.getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ), false );

            if( !existingDashboardIds.isEmpty() )
            {
                selectQuery.setWhereClause( new WhereClause( Criteria.notIn( visibilityTable.getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ),
                                                                             existingDashboardIds ) ).and( trashFilter ) );
            }
            else
            {
                selectQuery.setWhereClause( new WhereClause( trashFilter ) );
            }
            final DataContainer dataContainer = getSecureOrgDataStore( orgId ).get( selectQuery );

            dataContainer.getRows( visibilityTable )
                         .forEach( row ->
                                  {
                                      visibilityMap.computeIfAbsent( Visibility.get(row.get( OMCANADASHBOARDVISBILITY.VISIBILITY_ID ) ),
                                                                     visibility -> new HashMap<>() )
                                                   .put( row.getPKValue(),
                                                         row.get( OMCANADASHBOARDVISBILITY.DASH_ID ) );
                                  } );

            if( !visibilityMap.isEmpty() )
            {
                loadDashboardIdsByVisibility( orgId, userId, visibilityMap, dashboardIds );
                dashboardIds.removeIf( existingDashboardIds::contains );

                if( !dashboardIds.isEmpty() )
                {
                    dashboardDetails.addAll( getDashboardLayoutList( orgId,
                                                                     null,
                                                                     start,
                                                                     limit - dashboardDetailsSize,
                                                                     criteria,
                                                                     dashboardIds ) );
                }
            }
        }

        return dashboardDetails;
    }

    /**
     * <p>
     * Loads the dashboard ids based on the {@link Visibility} type
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param visibilityMap A key represents the visibility type and value represents the visibility id as key and dashboard id as value
     * @param dashboardIds  Contains the collection of dashboard ids
     */
    private void loadDashboardIdsByVisibility( final Long orgId,
                                               final Long userId,
                                               final Map<Visibility, Map<Long, Long>> visibilityMap,
                                               final Collection<Long> dashboardIds )
    {
        final Visibility justMeVisibility = Visibility.JUST_ME;
        final Visibility sharedVisibility = Visibility.SHARED_WITH;

        if( visibilityMap.containsKey( justMeVisibility ) )
        {
            loadDashboardIdsByUserVisibility( orgId, userId, visibilityMap.get( justMeVisibility ), dashboardIds );
        }

        if( visibilityMap.containsKey( sharedVisibility ) )
        {
            final Map<Long, Long> sharedVisibilityMap = visibilityMap.get( sharedVisibility );

            loadDashboardIdsByUserVisibility( orgId, userId, sharedVisibilityMap, dashboardIds );
            loadDashboardIdsByTeamVisibility( orgId, userId, sharedVisibilityMap, dashboardIds );
            loadDashboardIdsByTerritoryVisibility( orgId, userId, sharedVisibilityMap, dashboardIds );
            loadDashboardIdsByEveryoneVisibility( orgId, sharedVisibilityMap, dashboardIds );
        }
    }

    /**
     * <p>
     * Gets the collection of {@link DashboardLayout} details based on the given criteria and dashboard ids
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param userId        Represents the user id
     * @param start         Represents the starting index of dashboard to be fetched
     * @param limit         Represents the maximum number of dashboard to be fetched
     * @param criteria      Represents the {@link Criteria}
     * @param dashboardIds  Contains the collection of dashboard ids
     * @return The collection of {@link DashboardLayout}
     */
    private Collection<CRMDashboardDetails> getDashboardLayoutList( final Long orgId,
                                                                    final Long userId,
                                                                    final int start,
                                                                    final int limit,
                                                                    final DropdownCriteria criteria,
                                                                    final Collection<Long> dashboardIds )
    {
        final Collection<CRMDashboardDetails> dashboardLayouts = new HashSet<>();
        final Table dashboardDetailsTable = crmViewManagerAPI.getCRMDashboardManager()
                                                             .getTable();
        final SelectQuery selectQuery = SelectQuery.get( dashboardDetailsTable );
        final Column dashboardPkColumn = dashboardDetailsTable.getPKColumn();
        final Column displayNameColumn = dashboardDetailsTable.getColumn( CRMDASHBOARDDETAILS.CRM_VIEW_NAME );
        final WhereClause whereClause = new WhereClause( criteria.build( displayNameColumn ) );

        selectQuery.addSelectClause( List.of( dashboardPkColumn, displayNameColumn ) );
        selectQuery.setWhereClause( whereClause.and( Objects.nonNull( userId ) ? Criteria.eq( dashboardDetailsTable.getColumn( CRMDASHBOARDDETAILS.CREATED_BY ),
                                                                                              userId )
                                                                               : Criteria.in( dashboardPkColumn,
                                                                                              dashboardIds ) ).and( Criteria.eq( dashboardDetailsTable.getColumn( CRMDASHBOARDDETAILS.MARK_AS_TRASH ), false ) ) );
        selectQuery.setLimitClause( new LimitClause( start, limit ) );
        final DataContainer dataContainer = getSecureOrgDataStore( orgId ).get( selectQuery );

        dashboardLayouts.addAll( dataContainer.getRows( dashboardDetailsTable )
                                              .map( row -> CRMDashboardDetails.builder()
                                                                              .id( row.getPKValue() )
                                                                              .name( row.get( CRMDASHBOARDDETAILS.CRM_VIEW_NAME ) )
                                                                              .orgId( orgId )
                                                                              .build() )
                                              .toList() );

        return dashboardLayouts;
    }

    /**
     * <p>
     * Loads the dashboard ids based on the {@link DashboardTerritoryVisibility}
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param userId        Represents the user id
     * @param visibilityMap A key represents the visibility id and value represents the dashboard id
     * @param dashboardIds  Contains the collection of dashboard ids
     */
    private void loadDashboardIdsByTerritoryVisibility( final Long orgId,
                                                        final Long userId,
                                                        final Map<Long, Long> visibilityMap,
                                                        final Collection<Long> dashboardIds )
    {
        final Table territoryVisibilityTable = dashboardTerritoryVisibilityManager.getTable();
        final Table userTerritoryTable = Table.get( MCRMUSERTERRITORY.TABLE );
        final Table territoryTable = Table.get( OMCRMTERRITORY.TABLE );
        final SelectQuery selectQuery = SelectQuery.get( territoryVisibilityTable );
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final Column userTerritoryIdColumn = userTerritoryTable.getColumn( MCRMUSERTERRITORY.TERRITORY_ID );
        final Column userIdColumn = userTerritoryTable.getColumn( MCRMUSERTERRITORY.USER_ID );
        final Column visibilityIdColumn = territoryVisibilityTable.getColumn( MCANADTERRITORYVISBILITY.DASHBOARD_VISIBILITY_ID );
        final WhereClause whereClause = new WhereClause( Criteria.in( visibilityIdColumn,
                                                                      visibilityMap.keySet() ) )
                                                         .and( Criteria.notEq( territoryVisibilityTable.getColumn( MCANADTERRITORYVISBILITY.PERMISSION_ID ),
                                                                               VisibilityPermission.CAN_VIEW.getId() ) )
                                                         .and( Criteria.eq( userIdColumn, userId ) );

        selectQuery.addSelectClause( territoryVisibilityTable );
        selectQuery.addSelectClause( List.of( userTerritoryIdColumn, userIdColumn, territoryPkColumn ) );
        selectQuery.addJoinClause( List.of( new JoinClause( territoryVisibilityTable.getColumn( MCANADTERRITORYVISBILITY.TERRITORY_ID ),
                                                            territoryPkColumn ),
                                            new JoinClause( territoryPkColumn, userTerritoryIdColumn ) ) );
        selectQuery.setWhereClause( whereClause );

        buildDashboardIds( orgId, selectQuery, territoryVisibilityTable, MCANADTERRITORYVISBILITY.DASHBOARD_VISIBILITY_ID, dashboardIds, visibilityMap );
    }

    /**
     * <p>
     * Loads the dashboard ids based on the {@link DashboardEveryoneVisibility}
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param visibilityMap A key represents the visibility id and value represents the dashboard id
     * @param dashboardIds  Contains the collection of dashboard ids
     */
    private void loadDashboardIdsByEveryoneVisibility( final Long orgId,
                                                       final Map<Long, Long> visibilityMap,
                                                       final Collection<Long> dashboardIds )
    {
        final Table everyoneVisibilityTable = dashboardEveryoneVisibilityManager.getTable();
        final Column visibilityIdColumn = everyoneVisibilityTable.getColumn( MCANADEVERYONEVISBILITY.DASHBOARD_VISIBILITY_ID );
        final SelectQuery selectQuery = SelectQuery.get( everyoneVisibilityTable );
        final WhereClause whereClause = new WhereClause( Criteria.in( visibilityIdColumn,
                                                                      visibilityMap.keySet() ) )
                                                         .and( Criteria.notEq( everyoneVisibilityTable.getColumn( MCANADEVERYONEVISBILITY.PERMISSION_ID ),
                                                                               VisibilityPermission.CAN_VIEW.getId() ) );

        selectQuery.addSelectClause( everyoneVisibilityTable );
        selectQuery.setWhereClause( whereClause );

        buildDashboardIds( orgId, selectQuery, everyoneVisibilityTable, MCANADEVERYONEVISBILITY.DASHBOARD_VISIBILITY_ID, dashboardIds, visibilityMap );
    }

    /**
     * <p>
     * Loads the dashboard ids based on the {@link DashboardTeamVisibility}
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param userId        Represents the user id
     * @param visibilityMap A key represents the visibility id and value represents the dashboard id
     * @param dashboardIds  Contains the collection of dashboard ids
     */
    private void loadDashboardIdsByTeamVisibility( final Long orgId,
                                                   final Long userId,
                                                   final Map<Long, Long> visibilityMap,
                                                   final Collection<Long> dashboardIds )
    {
        final Table teamVisibilityTable = dashboardTeamVisibilityManager.getTable();
        final Table userTeamTable = Table.get( MCRMUSERTEAM.TABLE );
        final Table teamTable = Table.get( OMCRMTEAM.TABLE );
        final SelectQuery selectQuery = SelectQuery.get( teamVisibilityTable );
        final Column teamPkColumn = teamTable.getPKColumn();
        final Column userTeamIdColumn = userTeamTable.getColumn( MCRMUSERTEAM.TEAM_ID );
        final Column userIdColumn = userTeamTable.getColumn( MCRMUSERTEAM.USER_ID );
        final Column visibilityIdColumn = teamVisibilityTable.getColumn( MCANADTEAMVISBILITY.DASHBOARD_VISIBILITY_ID );
        final WhereClause whereClause = new WhereClause( Criteria.in( visibilityIdColumn,
                                                                      visibilityMap.keySet() ) )
                                                         .and( Criteria.notEq( teamVisibilityTable.getColumn( MCANADTEAMVISBILITY.PERMISSION_ID ),
                                                                               VisibilityPermission.CAN_VIEW.getId() ) )
                                                         .and( Criteria.eq( userIdColumn, userId ) );

        selectQuery.addSelectClause( teamVisibilityTable );
        selectQuery.addSelectClause( List.of( userTeamIdColumn, userIdColumn, teamPkColumn ) );
        selectQuery.addJoinClause( List.of( new JoinClause( teamVisibilityTable.getColumn( MCANADTEAMVISBILITY.TEAM_ID ), teamPkColumn ),
                                            new JoinClause( teamPkColumn, userTeamIdColumn ) ) );
        selectQuery.setWhereClause( whereClause );

        buildDashboardIds( orgId, selectQuery, teamVisibilityTable, MCANADTEAMVISBILITY.DASHBOARD_VISIBILITY_ID, dashboardIds, visibilityMap );
    }

    /**
     * <p>
     * Loads the dashboard ids based on the user {@link VisibilityPermission}
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param userId        Represents the user id
     * @param visibilityMap A key represents the visibility id and value represents the dashboard id
     * @param dashboardIds  Contains the collection of dashboard ids
     */
    private void loadDashboardIdsByUserVisibility( final Long orgId,
                                                   final Long userId,
                                                   final Map<Long, Long> visibilityMap,
                                                   final Collection<Long> dashboardIds )
    {
        final Table userVisibilityTable = dashboardUserVisibilityManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get( userVisibilityTable );

        selectQuery.addSelectClause( userVisibilityTable );
        selectQuery.setWhereClause( new WhereClause( Criteria.eq( userVisibilityTable.getColumn( MCANADUSERVISBILITY.USER_ID ),
                                                                  userId ) )
                                                     .and( Criteria.notEq( userVisibilityTable.getColumn( MCANADUSERVISBILITY.PERMISSION_ID ),
                                                                           VisibilityPermission.CAN_VIEW.getId() ) ) );

        buildDashboardIds( orgId, selectQuery, userVisibilityTable, MCANADUSERVISBILITY.DASHBOARD_VISIBILITY_ID, dashboardIds, visibilityMap );
    }

    /**
     * <p>
     * Builds the dashboard ids by given select query
     * </p>
     *
     * @param orgId         Represents an organisation id
     * @param selectQuery   Represents the {@link SelectQuery}
     * @param visibilityMap A map key contains the visibility id and value contains the dashboard id
     * @param columnName    Represents the column name
     * @param dashboardIds  Contains the collection of dashboard ids
     * @param table         Represents the table name
     */
    private void buildDashboardIds( final Long orgId,
                                    final SelectQuery selectQuery,
                                    final Table table,
                                    final String columnName,
                                    final Collection<Long> dashboardIds,
                                    final Map<Long, Long> visibilityMap )
    {
        dashboardIds.addAll( getSecureOrgDataStore( orgId ).get( selectQuery )
                                                           .getRows( table )
                                                           .map( row -> NumberUtils.parseNumber( row.get( columnName ) )
                                                                                   .longValue() )
                                                           .map( visibilityMap::get )
                                                           .filter( Objects::nonNull )
                                                           .toList() );

    }

    /**
     * <p>
     * Gets the secure {@link OrgDataStore} based on the given organization id
     * </p>
     *
     * @param orgId Represents an organisation id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore getSecureOrgDataStore( final long orgId )
    {
        return DataStoreProvider.getSecureOrgDataStore( orgId );
    }
}

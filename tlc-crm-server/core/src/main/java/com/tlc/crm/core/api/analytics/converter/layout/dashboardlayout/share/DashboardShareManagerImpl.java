package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.share;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.Counter;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.core.api.analytics.entity.share.DashboardEveryoneVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTeamVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardTerritoryVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardUserVisibility;
import com.tlc.crm.core.api.analytics.entity.share.DashboardVisibility;
import com.tlc.crm.core.api.analytics.entity.share.Visibility;
import com.tlc.crm.core.api.analytics.entity.share.VisibilityPermission;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.MCANADEVERYONEVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTEAMVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADTERRITORYVISBILITY;
import com.tlc.crm.core.sql.resource.MCANADUSERVISBILITY;
import com.tlc.crm.core.sql.resource.OMCANADASHBOARDVISBILITY;

import com.tlc.i18n.I18nResolver;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;

import lombok.NonNull;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides an abstract way for share dashboard
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Accessors(fluent = true)
class DashboardShareManagerImpl implements DashboardVisibilityService
{
    private final CRMReportManagerAPI crmReportManagerAPI;
    private final UserService userService;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;
    private static final Logger LOGGER = LoggerFactory.getLogger( DashboardShareManagerImpl.class );

    private DashboardShareManagerImpl()
    {
        crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        userService = UserService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
        crmReportManagerAPI.getDashBoardEveryoneVisibilityManager();
         crmReportManagerAPI.getDashBoardTeamVisibilityManager();
         crmReportManagerAPI.getDashBoardTerritoryVisibilityManager();
         crmReportManagerAPI.getDashBoardUserVisibilityManager();
    }

    private static class Instance
    {
        private static final DashboardShareManagerImpl INSTANCE = new DashboardShareManagerImpl();
    }

    public static DashboardShareManagerImpl INSTANCE()
    {
        return DashboardShareManagerImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param request          A JSON object containing the sharing details
     * @param i18nResolver     An instance of {@link I18nResolver} for handling localization of messages
     * @return A {@link JsonObject} containing the response of the share dashboard
     */
    @Override
    public @NonNull JsonObject share( final @NonNull AnalyticsRequest analyticsRequest,
                                      final @NonNull JsonObject request,
                                      final @NonNull I18nResolver i18nResolver )
    {
        final long orgId = analyticsRequest.orgId();
        final long userId = analyticsRequest.userId();
        final JsonObject jsonObject = Json.object();
        final long dashboardId = request.getLong( CRMReportKeys.DASHBOARD_ID );
        final short visibilityId = request.getShort( CRMReportKeys.VISIBILITY_ID );
        final Visibility visibility = Visibility.get(visibilityId);
        final JsonObject dashboardConfig = getShareVisibility( analyticsRequest, request, i18nResolver );
        final Collection<VisibilityPermission> accessiblePermissions = new HashSet<>();

        if( dashboardConfig.containsKey( CRMDashboardKeys.CURRENT_USER_CONFIG ) )
        {
            final JsonArray currentUserConfig = dashboardConfig.getJsonArray( CRMDashboardKeys.CURRENT_USER_CONFIG );
            final int totalSize = currentUserConfig.size();

            for( int index = 0; index < totalSize; index++ )
            {
                accessiblePermissions.add( VisibilityPermission.get( (short) currentUserConfig.get( index ) ) );
            }
        }
        else
        {
            accessiblePermissions.addAll( getUserAccessiblePermissions( VisibilityPermission.OWNER ) );
        }

        switch( visibility )
        {
            case JUST_ME ->
            {
                final DashboardVisibility dashboardVisibility = getDashboardVisibility( orgId, dashboardId );
                final Long dashboardVisibilityId = dashboardVisibility.id();

                deleteEveryoneVisibilityDetails( orgId, dashboardVisibilityId );
                deleteUserVisibilityDetails( orgId, dashboardVisibilityId );
                deleteTeamVisibilityDetails( orgId, dashboardVisibilityId );
                deleteTerritoryVisibilityDetails( orgId, dashboardVisibilityId );
                updateDashboardVisibility( orgId, userId, dashboardId, visibilityId, accessiblePermissions );
            }
            case SHARED_WITH ->
            {
                final DashboardVisibility dashboardVisibility = getDashboardVisibility( orgId, dashboardId );
                final Long dashboardVisibilityId = dashboardVisibility.id();

                if( request.containsKey( CRMDashboardKeys.USERS ) )
                {
                    updateDashboardUserVisibility( orgId,
                                                   dashboardVisibilityId,
                                                   request.getJsonArray(CRMDashboardKeys.USERS),
                                                   accessiblePermissions );
                }

                if( request.containsKey( CRMDashboardKeys.TEAMS ) )
                {
                    updateDashboardTeamVisibility( orgId,
                                                   dashboardVisibilityId,
                                                   request.getJsonArray( CRMDashboardKeys.TEAMS ),
                                                   accessiblePermissions );
                }

                if( request.containsKey( CRMDashboardKeys.TERRITORIES ) )
                {
                    updateDashboardTerritoryVisibility( orgId,
                                                        dashboardVisibilityId,
                                                        request.getJsonArray( CRMDashboardKeys.TERRITORIES ),
                                                        accessiblePermissions );
                }

                if( request.containsKey( CRMDashboardKeys.EVERYONE ) )
                {
                    final JsonObject everyoneJson = request.getJsonObject( CRMDashboardKeys.EVERYONE );

                    updateDashboardEveryoneVisibility( orgId,
                                                       everyoneJson.getShort( CRMDashboardKeys.ACCESS ),
                                                       dashboardVisibilityId,
                                                       accessiblePermissions );
                }

                updateDashboardVisibility( orgId, userId, dashboardId, visibilityId, accessiblePermissions );
            }
        }

        return jsonObject.put( CommonKeys.SUCCESS, "Dashboard shared successfully" );
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param jsonObject       A JSON object containing request parameters, such as the dashboard id
     * @param i18nResolver     An instance of {@link I18nResolver} for handling localization of messages
     * @return A {@link JsonObject} containing the visibility details of the dashboard
     */
    @Override
    public @NonNull JsonObject getShareVisibility( final @NonNull AnalyticsRequest analyticsRequest,
                                                   final @NonNull JsonObject jsonObject,
                                                   final @NonNull I18nResolver i18nResolver )
    {
        final Long orgId = analyticsRequest.orgId();
        final Long userId = analyticsRequest.userId();
        final Long dashboardId = jsonObject.getLong( CRMReportKeys.DASHBOARD_ID );
        final CRMViewConfigManager<DashboardVisibility> dashboardVisibilityManager = crmReportManagerAPI.getDashBoardVisibilityManager();
        final Criteria criteria = Criteria.eq( dashboardVisibilityManager.getTable()
                                                                        .getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ),
                                               dashboardId );
        final JsonObject response = Json.object();

        try
        {
            final Collection<DashboardVisibility> dashboardVisibilities = dashboardVisibilityManager.getAll( orgId, criteria );

            if( Objects.nonNull( dashboardVisibilities ) && !dashboardVisibilities.isEmpty() )
            {
                final DashboardVisibility dashboardVisibility = dashboardVisibilities.stream()
                                                                                     .findAny()
                                                                                     .get();
                final Visibility visibility = dashboardVisibility.getVisibility();

                switch( visibility )
                {
                    case JUST_ME ->
                    {
                        response.put( CRMDashboardKeys.VISIBILITY_ID, visibility.getId() );
                    }
                    case SHARED_WITH ->
                    {
                        final Long dashboardVisibilityId = dashboardVisibility.id();
                        final Collection<Long> userIds = new HashSet<>();
                        final Collection<VisibilityPermission> currentUserPermission = new HashSet<>();

                        populateEveryoneVisibilityDetails( orgId, dashboardVisibilityId, currentUserPermission, response );
                        populateUserVisibilityDetails( orgId, userId, dashboardVisibilityId, userIds, currentUserPermission, response );
                        populateTeamVisibilityDetails( orgId, userId, dashboardVisibilityId, userIds, currentUserPermission, response );
                        populateTerritoryVisibilityDetails( orgId, userId, dashboardVisibilityId, userIds, currentUserPermission, response );

                        if( !currentUserPermission.isEmpty() )
                        {
                            final JsonArray jsonArray = Json.array();

                            for( final VisibilityPermission visibilityPermission : currentUserPermission )
                            {
                                jsonArray.put( visibilityPermission.getId() );
                            }
                            response.put( CRMDashboardKeys.CURRENT_USER_CONFIG, jsonArray );
                        }
                        response.put( CRMDashboardKeys.VISIBILITY_ID, visibility.getId() );
                    }
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                 "i18n.crm.analytics.visibility.permission.invalid" );
        }

        return response;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organisation id
     * @param userId      Represents the user id
     * @param dashboardId Represents the dashboard id
     * @param dataContainer    Holds the rows that will get committed
     */
    @Override
    public void createVisibility( final @NonNull Long orgId,
                                  final @NonNull Long userId,
                                  final @NonNull Long dashboardId,
                                  final DataContainer dataContainer )
    {
        final DashboardVisibility dashboardVisibility = DashboardVisibility.builder()
                                                                           .orgId( orgId )
                                                                           .dashId( dashboardId )
                                                                           .visibilityId( Visibility.JUST_ME.getId() )
                                                                           .createdBy( userId )
                                                                           .modifiedBy( userId )
                                                                           .build();

        try
        {
            crmReportManagerAPI.getDashBoardVisibilityManager()
                          .loadDataIntoContainer( List.of( dashboardVisibility ), dataContainer );
            LOGGER.info( "CRM Analytics: Successfully loaded values into the container for orgID: {}", orgId );
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgID: {}. Reason: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }
    }

    /**
     * <p>
     * Updates the visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param userId                Represents the user id
     * @param dashboardId           Represents the dashboard id
     * @param visibilityId          Represents the dashboard visibility id
     * @param accessiblePermissions Represents the {@link Collection} of {@link VisibilityPermission} which holds the accessible visibility
     */
    private void updateDashboardVisibility( final long orgId,
                                            final long userId,
                                            final long dashboardId,
                                            final short visibilityId,
                                            final Collection<VisibilityPermission> accessiblePermissions )
    {
        try
        {
            final DashboardVisibility entityDashboardVisibility = getDashboardVisibility( orgId, dashboardId );

            if( Visibility.JUST_ME == entityDashboardVisibility.getVisibility() && ( !accessiblePermissions.contains( VisibilityPermission.OWNER ) ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                     "i18n.crm.analytics.visibility.permission.invalid" );
            }

            final Long dashboardVisibilityId = entityDashboardVisibility.id();
            final DashboardVisibility sourceDashboardVisibility = DashboardVisibility.builder()
                                                                                     .orgId( orgId )
                                                                                     .dashId( dashboardId )
                                                                                     .visibilityId( visibilityId )
                                                                                     .id( dashboardVisibilityId )
                                                                                     .modifiedBy( userId )
                                                                                     .createdBy( entityDashboardVisibility.getCreatedBy() )
                                                                                     .build();

            crmReportManagerAPI.getDashBoardVisibilityManager()
                          .update( sourceDashboardVisibility );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
    }

    /**
     * <p>
     * Updates the user visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param users                 Represents the user details
     * @param accessiblePermissions Represents the {@link Collection} of {@link VisibilityPermission} which holds the accessible visibility
     */
    private void updateDashboardUserVisibility( final long orgId,
                                                final long dashboardVisibilityId,
                                                final JsonArray users,
                                                final Collection<VisibilityPermission> accessiblePermissions )
    {
        try
        {
            final Map<Long, VisibilityPermission> dashboardUserPermissions = getDashboardVisibilityPermission( users );
            final Collection<DashboardUserVisibility> entityDashboardUserVisibilities = getDashboardUserVisibility( orgId, dashboardVisibilityId );
            final CRMViewConfigManager<DashboardUserVisibility> dashboardUserVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                            .getDashBoardUserVisibilityManager();
            final Collection<DashboardUserVisibility> dashboardUserVisibilities = new HashSet<>();
            final Collection<DashboardUserVisibility> unMappedDashboardUsers = new HashSet<>();
            final Collection<Long> ownerIds = new HashSet<>();

            CodeBlockExecutor.forEachSafely( entityDashboardUserVisibilities,
                         entityDashboardUserVisibility ->
                                             {
                                                 final Long existingUserId = entityDashboardUserVisibility.getUserId();

                                                 if( dashboardUserPermissions.containsKey( existingUserId ) )
                                                 {
                                                     final VisibilityPermission visibilityPermission = dashboardUserPermissions.get( existingUserId );

                                                     if( !accessiblePermissions.contains( visibilityPermission ) )
                                                     {
                                                         throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                                                              "i18n.crm.analytics.visibility.permission.invalid" );
                                                     }

                                                     if( entityDashboardUserVisibility.getVisibilityPermission() == visibilityPermission )
                                                     {
                                                         if( VisibilityPermission.OWNER == visibilityPermission )
                                                         {
                                                             ownerIds.add(existingUserId);
                                                         }
                                                         dashboardUserPermissions.remove(existingUserId);
                                                     }
                                                     else
                                                     {
                                                         if( !ownerIds.isEmpty() && VisibilityPermission.OWNER == visibilityPermission )
                                                         {
                                                             throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                                                         "i18n.crm.analytics.visibility.permission.invalid" );
                                                         }

                                                         unMappedDashboardUsers.add( entityDashboardUserVisibility );
                                                     }
                                                 }
                                                 else
                                                 {
                                                     unMappedDashboardUsers.add( entityDashboardUserVisibility );
                                                 }
                                             });

            if( !unMappedDashboardUsers.isEmpty() )
            {
                dashboardUserVisibilityManager.delete( orgId, unMappedDashboardUsers );
            }

            if( !dashboardUserPermissions.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( dashboardUserPermissions.entrySet(),
                     dashboardUserPermission ->
                                                {
                                                    dashboardUserVisibilities.add( DashboardUserVisibility.builder()
                                                                                                          .orgId( orgId )
                                                                                                          .userId( dashboardUserPermission.getKey() )
                                                                                                          .dashboardVisibilityId( dashboardVisibilityId )
                                                                                                          .visibilityPermissionId( dashboardUserPermission.getValue()
                                                                                                                                                          .getId() )
                                                                                                          .build() );
                                                } );
            }

            if( !dashboardUserVisibilities.isEmpty() )
            {
                dashboardUserVisibilityManager.create( orgId, dashboardUserVisibilities );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Updates the team visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param teams                 Represents the team details
     * @param accessiblePermissions Represents the {@link Collection} of {@link VisibilityPermission} which holds the accessible visibility
     */
    private void updateDashboardTeamVisibility( final long orgId,
                                                final long dashboardVisibilityId,
                                                final JsonArray teams,
                                                final Collection<VisibilityPermission> accessiblePermissions )
    {
        try
        {
            final Map<Long, VisibilityPermission> dashboardTeamPermissions = getDashboardVisibilityPermission( teams );
            final Collection<DashboardTeamVisibility> entityDashboardTeamVisibilities = getDashboardTeamVisibility( orgId, dashboardVisibilityId );
            final CRMViewConfigManager<DashboardTeamVisibility> dashboardTeamVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                            .getDashBoardTeamVisibilityManager();
            final Collection<DashboardTeamVisibility> dashboardTeamVisibilities = new HashSet<>();

            if( Objects.nonNull( entityDashboardTeamVisibilities ) && !entityDashboardTeamVisibilities.isEmpty() )
            {
                final Collection<DashboardTeamVisibility> unMappedDashboardTeams = new HashSet<>();

                CodeBlockExecutor.forEachSafely( entityDashboardTeamVisibilities,
                            entityDashboardTeamVisibility ->
                                                 {
                                                     final Long existingTeamId = entityDashboardTeamVisibility.getTeamId();

                                                     if( dashboardTeamPermissions.containsKey( existingTeamId ) )
                                                     {
                                                         final VisibilityPermission visibilityPermission = dashboardTeamPermissions.get( existingTeamId );

                                                         if( !accessiblePermissions.contains( visibilityPermission ) )
                                                         {
                                                             throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                                                                 "i18n.crm.analytics.visibility.permission.invalid" );
                                                         }

                                                         if( entityDashboardTeamVisibility.getVisibilityPermission() == visibilityPermission )
                                                         {
                                                             dashboardTeamPermissions.remove( existingTeamId );
                                                         }
                                                         else
                                                         {
                                                             unMappedDashboardTeams.add( entityDashboardTeamVisibility );
                                                         }
                                                     }
                                                     else
                                                     {
                                                         unMappedDashboardTeams.add( entityDashboardTeamVisibility );
                                                     }
                                                 } );

                if( !unMappedDashboardTeams.isEmpty() )
                {
                    dashboardTeamVisibilityManager.delete( orgId, unMappedDashboardTeams );
                }
            }

            if( !dashboardTeamPermissions.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( dashboardTeamPermissions.entrySet(),
                     dashboardTeamPermission ->
                                                 {
                                                     dashboardTeamVisibilities.add( DashboardTeamVisibility.builder()
                                                                                                           .orgId( orgId )
                                                                                                           .teamId( dashboardTeamPermission.getKey() )
                                                                                                           .dashboardVisibilityId( dashboardVisibilityId )
                                                                                                           .visibilityPermissionId( dashboardTeamPermission.getValue()
                                                                                                                                                           .getId())
                                                                                                           .build() );
                                                 } );

                if( !dashboardTeamVisibilities.isEmpty() )
                {
                    dashboardTeamVisibilityManager.create( orgId, dashboardTeamVisibilities );
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Updates the territory visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param territories           Represents the territories details
     * @param accessiblePermissions Represents the {@link Collection} of {@link VisibilityPermission} which holds the accessible visibility
     */
    private void updateDashboardTerritoryVisibility( final long orgId,
                                                     final long dashboardVisibilityId,
                                                     final JsonArray territories,
                                                     final Collection<VisibilityPermission> accessiblePermissions )
    {
        try
        {
            final Map<Long, VisibilityPermission> dashboardTerritoryPermissions = getDashboardVisibilityPermission( territories );
            final Collection<DashboardTerritoryVisibility> entityDashboardTerritoryVisibilities = getDashboardTerritoryVisibility( orgId, dashboardVisibilityId );
            final CRMViewConfigManager<DashboardTerritoryVisibility> dashboardTerritoryVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                                      .getDashBoardTerritoryVisibilityManager();
            final Collection<DashboardTerritoryVisibility> dashboardTerritoryVisibilities = new HashSet<>();

            if( Objects.nonNull( entityDashboardTerritoryVisibilities ) && !entityDashboardTerritoryVisibilities.isEmpty() )
            {
                final Collection<DashboardTerritoryVisibility> unMappedDashboardTerritories = new HashSet<>();

                CodeBlockExecutor.forEachSafely( entityDashboardTerritoryVisibilities,
                         entityDashboardTerritoryVisibility ->
                                                {
                                                    final Long existingTerritoryId = entityDashboardTerritoryVisibility.getTerritoryId();

                                                    if( dashboardTerritoryPermissions.containsKey( existingTerritoryId ) )
                                                    {
                                                        final VisibilityPermission visibilityPermission = dashboardTerritoryPermissions.get( existingTerritoryId );

                                                        if( !accessiblePermissions.contains( visibilityPermission ) )
                                                        {
                                                            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                                                                 "i18n.crm.analytics.visibility.permission.invalid" );
                                                        }

                                                        if( entityDashboardTerritoryVisibility.getVisibilityPermission() == visibilityPermission )
                                                        {
                                                            dashboardTerritoryPermissions.remove( existingTerritoryId );
                                                        }
                                                        else
                                                        {
                                                            unMappedDashboardTerritories.add( entityDashboardTerritoryVisibility );
                                                        }
                                                    }
                                                    else
                                                    {
                                                        unMappedDashboardTerritories.add( entityDashboardTerritoryVisibility );
                                                    }
                                                } );

                if( !unMappedDashboardTerritories.isEmpty() )
                {
                    dashboardTerritoryVisibilityManager.delete( orgId, unMappedDashboardTerritories );
                }
            }

            if( !dashboardTerritoryPermissions.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( dashboardTerritoryPermissions.entrySet(),
                     dashboardTerritoryPermission ->
                                                {
                                                    dashboardTerritoryVisibilities.add( DashboardTerritoryVisibility.builder()
                                                                                                                    .orgId( orgId )
                                                                                                                    .territoryId( dashboardTerritoryPermission.getKey() )
                                                                                                                    .dashboardVisibilityId( dashboardVisibilityId )
                                                                                                                    .visibilityPermissionId( dashboardTerritoryPermission.getValue()
                                                                                                                                                                         .getId() )
                                                                                                                    .build() );
                                                } );

                dashboardTerritoryVisibilityManager.create( orgId, dashboardTerritoryVisibilities );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Updates the user visibility configuration for a specific dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param accessId              Represents the visibility access id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param accessiblePermissions Represents the {@link Collection} of {@link VisibilityPermission} which holds the accessible visibility
     */
    private void updateDashboardEveryoneVisibility( final long orgId,
                                                    final short accessId,
                                                    final long dashboardVisibilityId,
                                                    final Collection<VisibilityPermission> accessiblePermissions )
    {
        try
        {
            final Map<Long, VisibilityPermission> dashboardEveryonePermissions = new HashMap<>();
            final Collection<DashboardEveryoneVisibility> unMappedDashboardEveryone = new HashSet<>();
            final Collection<DashboardEveryoneVisibility> dashboardEveryoneVisibilities = new HashSet<>();
            final CRMViewConfigManager<DashboardEveryoneVisibility> dashboardEveryoneVisibilityManager = crmReportManagerAPI.getDashBoardEveryoneVisibilityManager();
            final Collection<DashboardEveryoneVisibility> entityDashboardEveryoneVisibilities = getDashboardEveryoneVisibility( orgId, dashboardVisibilityId );

            dashboardEveryonePermissions.put( dashboardVisibilityId, VisibilityPermission.get( accessId ) );

            if( Objects.nonNull( entityDashboardEveryoneVisibilities ) && !entityDashboardEveryoneVisibilities.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( entityDashboardEveryoneVisibilities,
                         entityDashboardEveryoneVisibility ->
                                                {
                                                    final Long existingId = entityDashboardEveryoneVisibility.getDashboardVisibilityId();

                                                    if( dashboardEveryonePermissions.containsKey( existingId ) )
                                                    {
                                                        final VisibilityPermission visibilityPermission = dashboardEveryonePermissions.get( existingId );

                                                        if( !accessiblePermissions.contains( visibilityPermission ) )
                                                        {
                                                            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                                                                 "i18n.crm.analytics.visibility.permission.invalid" );
                                                        }

                                                        if( entityDashboardEveryoneVisibility.getVisibilityPermission() == visibilityPermission )
                                                        {
                                                            dashboardEveryonePermissions.remove( existingId );
                                                        }
                                                        else
                                                        {
                                                            unMappedDashboardEveryone.add( entityDashboardEveryoneVisibility );
                                                        }
                                                    }
                                                    else
                                                    {
                                                        unMappedDashboardEveryone.add( entityDashboardEveryoneVisibility );
                                                    }

                                                } );

                if( !unMappedDashboardEveryone.isEmpty() )
                {
                    dashboardEveryoneVisibilityManager.delete( orgId, unMappedDashboardEveryone );
                }
            }

            if( !dashboardEveryonePermissions.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( dashboardEveryonePermissions.entrySet(),
                     dashboardEveryonePermission ->
                                                {
                                                    dashboardEveryoneVisibilities.add( DashboardEveryoneVisibility.builder()
                                                                                                                  .orgId( orgId )
                                                                                                                  .dashboardVisibilityId( dashboardEveryonePermission.getKey() )
                                                                                                                  .visibilityPermissionId( dashboardEveryonePermission.getValue()
                                                                                                                                                                      .getId() )
                                                                                                                  .build() );
                                                } );

                dashboardEveryoneVisibilityManager.create( orgId, dashboardEveryoneVisibilities );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Retrieves the dashboard visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param jsonArray Represents the dashboard visibility details
     * @return A {@link Map} containing the mapping of dashboard permission id to their respective values
     */
    private Map<Long, VisibilityPermission> getDashboardVisibilityPermission( final JsonArray jsonArray )
    {
        final Map<Long, VisibilityPermission> dashboardVisibilityPermissions = new HashMap<>();
        final int totalSize = jsonArray.size();

        for(int index = 0; index < totalSize; index++ )
        {
            final JsonObject jsonObject = jsonArray.getJsonObject( index );

            dashboardVisibilityPermissions.put( jsonObject.getLong( CRMReportKeys.ID ),
                                                VisibilityPermission.get( jsonObject.getShort( CRMDashboardKeys.ACCESS ) ) );
        }

        return dashboardVisibilityPermissions;
    }

    /**
     * <p>
     * Populates the user visibility details for a dashboard into the provided JSON response object
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param userId                Represents the user id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param userIds               Represents the {@link Collection} which holds the user ids
     * @param currentUserPermission Represents the {@link Collection} of {@link VisibilityPermission} holds the current user visibility permission details
     * @param response              the Json object where the visibility details will be populated
     */
    private void populateUserVisibilityDetails( final Long orgId,
                                                final Long userId,
                                                final Long dashboardVisibilityId,
                                                final Collection<Long> userIds,
                                                final Collection<VisibilityPermission> currentUserPermission,
                                                final JsonObject response )
    {
        try
        {
            final Collection<DashboardUserVisibility> dashboardUserVisibilities = getDashboardUserVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardUserVisibilities ) && !dashboardUserVisibilities.isEmpty() )
            {
                final Map<Long, VisibilityPermission> userVisibilityPermissions = new HashMap<>();
                final JsonArray userArray = Json.array();

                CodeBlockExecutor.forEachSafely( dashboardUserVisibilities,
                             dashboardUserVisibility ->
                             {
                                 final Long id = dashboardUserVisibility.getUserId();
                                 final VisibilityPermission visibilityPermission = dashboardUserVisibility.getVisibilityPermission();

                                 if( Objects.equals( userId, id ) )
                                 {
                                     currentUserPermission.addAll( getUserAccessiblePermissions( visibilityPermission ) );
                                 }
                                 userVisibilityPermissions.put( id, visibilityPermission);
                             } );

                if( !userVisibilityPermissions.isEmpty() )
                {
                    final Collection<CrmUser> crmUsers = userService.getUsers( orgId, userVisibilityPermissions.keySet() );

                    if( Objects.nonNull( crmUsers ) && ! crmUsers.isEmpty() )
                    {
                        CodeBlockExecutor.forEachSafely( crmUsers,
                                                         crmUser ->
                                                         {
                                                             final JsonObject userJson = Json.object();
                                                             final Long id = crmUser.id();

                                                             userJson.put( CRMReportKeys.ID, id );
                                                             userJson.put( CRMDashboardKeys.ACCESS, userVisibilityPermissions.get( id )
                                                                                                                             .getId() );
                                                             userIds.add( id );
                                                             userArray.put( userJson );
                                                         } );

                    }
                }

                if( 0 < userArray.size() )
                {
                    response.put( CRMDashboardKeys.USERS, userArray );
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Populates the team visibility details for a dashboard into the provided JSON response object
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param userId                Represents the user id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param userIds               Represents the {@link Collection} which holds the user ids
     * @param currentUserPermission Represents the {@link Collection} of {@link VisibilityPermission} holds the current user visibility permission details
     * @param response              the Json object where the visibility details will be populated
     */
    private void populateTeamVisibilityDetails( final Long orgId,
                                                final Long userId,
                                                final Long dashboardVisibilityId,
                                                final Collection<Long> userIds,
                                                final Collection<VisibilityPermission> currentUserPermission,
                                                final JsonObject response )
    {
        try
        {
            final Collection<DashboardTeamVisibility> dashboardTeamVisibilities = getDashboardTeamVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardTeamVisibilities ) && !dashboardTeamVisibilities.isEmpty() )
            {
                final Map<Long, VisibilityPermission> teamVisibilityPermissions = new HashMap<>();
                final JsonArray teamArray = Json.array();

                CodeBlockExecutor.forEachSafely( dashboardTeamVisibilities,
                            dashboardTeamVisibility ->
                                                 {
                                                     teamVisibilityPermissions.put( dashboardTeamVisibility.getTeamId(),
                                                                                    dashboardTeamVisibility.getVisibilityPermission());
                                                 } );

                if( !teamVisibilityPermissions.isEmpty() )
                {
                    final Collection<Team> teams = teamManager.getTeams( orgId, teamVisibilityPermissions.keySet(), false );

                    if( Objects.nonNull( teams ) && !teams.isEmpty() )
                    {
                        CodeBlockExecutor.forEachSafely( teams,
                                                         team ->
                                                         {
                                                             final JsonObject teamJson = Json.object();
                                                             final Long teamId = team.id();
                                                             final Collection<TeamUser> teamUsers = team.getUsers();
                                                             final Counter counter = new Counter();

                                                             if( Objects.nonNull( teamUsers ) && ! teamUsers.isEmpty() )
                                                             {
                                                                 CodeBlockExecutor.forEachSafely( teamUsers,
                                                                                                  teamUser ->
                                                                                                  {
                                                                                                      final CrmUser crmUser = teamUser.getCrmUser();

                                                                                                      if( Objects.nonNull( crmUser ) && !userIds.contains( crmUser.id() ) )
                                                                                                      {
                                                                                                          counter.getAndIncrement();
                                                                                                      }

                                                                                                      if( Objects.equals( userId, crmUser.id() ) )
                                                                                                      {
                                                                                                          currentUserPermission.addAll( getUserAccessiblePermissions( teamVisibilityPermissions.get( teamId ) ) );
                                                                                                      }
                                                                                                  } );
                                                                 final int totalTeamMembers = counter.getCount();

                                                                 if( 0 < totalTeamMembers  )
                                                                 {
                                                                     teamJson.put( CommonKeys.COUNT, totalTeamMembers );
                                                                 }
                                                             }

                                                             teamJson.put( CRMReportKeys.ID, teamId );
                                                             teamJson.put( CommonKeys.NAME, team.getName() );
                                                             teamJson.put( CRMDashboardKeys.ACCESS, teamVisibilityPermissions.get( teamId )
                                                                                                                             .getId() );
                                                             teamArray.put( teamJson );
                                                         } );
                    }
                }

                if( 0 < teamArray.size() )
                {
                    response.put( CRMDashboardKeys.TEAMS, teamArray );
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Populates the territory visibility details for a dashboard into the provided JSON response object
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param userId                Represents the user id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param userIds               Represents the {@link Collection} which holds the user ids
     * @param currentUserPermission Represents the {@link Collection} of {@link VisibilityPermission} holds the current user visibility permission details
     * @param response              the Json object where the visibility details will be populated
     */
    private void populateTerritoryVisibilityDetails( final Long orgId,
                                                     final Long userId,
                                                     final Long dashboardVisibilityId,
                                                     final Collection<Long> userIds,
                                                     final Collection<VisibilityPermission> currentUserPermission,
                                                     final JsonObject response )
    {
        try
        {
            final Collection<DashboardTerritoryVisibility> dashboardTerritoryVisibilities = getDashboardTerritoryVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardTerritoryVisibilities ) && !dashboardTerritoryVisibilities.isEmpty() )
            {
                final Map<Long, VisibilityPermission> territoryVisibilityPermissions = new HashMap<>();
                final JsonArray territoryArray = Json.array();

                CodeBlockExecutor.forEachSafely( dashboardTerritoryVisibilities,
                         dashboardTerritoryVisibility ->
                                                {
                                                    territoryVisibilityPermissions.put( dashboardTerritoryVisibility.getTerritoryId(),
                                                                                        dashboardTerritoryVisibility.getVisibilityPermission());
                                                } );

                if( !territoryVisibilityPermissions.isEmpty() )
                {
                    final Collection<Territory> territories = territoryManager.getTerritories( orgId, territoryVisibilityPermissions.keySet(), false );

                    if( Objects.nonNull( territories ) && !territories.isEmpty() )
                    {
                        CodeBlockExecutor.forEachSafely( territories,
                                                         territory ->
                                                         {
                                                             final JsonObject territoryJson = Json.object();
                                                             final Long territoryId = territory.id();
                                                             final Collection<CrmUser> crmUsers = territory.getCrmUsers();
                                                             final Collection<Team> teamUsers = territory.getTeams();
                                                             final Counter counter = new Counter();

                                                             SafeConsumer<Long> userCountHandler = (id ) ->
                                                             {
                                                                 if( !userIds.contains( id ) )
                                                                 {
                                                                     counter.getAndIncrement();
                                                                 }
                                                             };

                                                             if( Objects.nonNull( crmUsers ) && ! crmUsers.isEmpty() )
                                                             {
                                                                 CodeBlockExecutor.forEachSafely( crmUsers,
                                                                                                  crmUser ->
                                                                                                  {
                                                                                                      final Long id = crmUser.id();

                                                                                                      if( Objects.equals( userId, crmUser.id() ) )
                                                                                                      {
                                                                                                          currentUserPermission.addAll( getUserAccessiblePermissions( territoryVisibilityPermissions.get( territoryId ) ) );
                                                                                                      }
                                                                                                      userCountHandler.accept( id );
                                                                                                  } );
                                                             }

                                                             if( Objects.nonNull( teamUsers ) && !teamUsers.isEmpty() )
                                                             {
                                                                 CodeBlockExecutor.forEachSafely( teamUsers,
                                                                                                  teamUser ->
                                                                                                  {
                                                                                                      final Collection<TeamUser> teamCrmUsers = teamUser.getUsers();

                                                                                                      CodeBlockExecutor.forEachSafely( teamCrmUsers,
                                                                                                                                       teamCrmUser ->
                                                                                                                                       {
                                                                                                                                           final CrmUser crmUser = teamCrmUser.getCrmUser();

                                                                                                                                           if( Objects.nonNull( crmUser ) )
                                                                                                                                           {
                                                                                                                                               final Long id = crmUser.id();

                                                                                                                                               if( Objects.equals( userId, id ) )
                                                                                                                                               {
                                                                                                                                                   currentUserPermission.addAll( getUserAccessiblePermissions( territoryVisibilityPermissions.get( territoryId ) ) );
                                                                                                                                               }
                                                                                                                                               userCountHandler.accept( id );
                                                                                                                                           }
                                                                                                                                       } );
                                                                                                  } );
                                                             }
                                                             final int totalUsers = counter.getCount();

                                                             if( 0 < totalUsers)
                                                             {
                                                                 territoryJson.put( CommonKeys.COUNT, totalUsers );
                                                             }
                                                             territoryJson.put( CRMReportKeys.ID, territoryId );
                                                             territoryJson.put( CommonKeys.NAME, territory.getName() );
                                                             territoryJson.put( CRMDashboardKeys.ACCESS, territoryVisibilityPermissions.get( territoryId )
                                                                                                                                       .getId() );
                                                             territoryArray.put( territoryJson );
                                                         } );
                    }
                }

                if( 0 < territoryArray.size() )
                {
                    response.put( CRMDashboardKeys.TERRITORIES, territoryArray );
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                    "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Populates everyone visibility details for a dashboard into the provided JSON response object
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @param currentUserPermission Represents the {@link Collection} of {@link VisibilityPermission} holds the current user visibility permission details
     * @param response              the Json object where the visibility details will be populated
     */
    private void populateEveryoneVisibilityDetails( final Long orgId,
                                                    final Long dashboardVisibilityId,
                                                    final Collection<VisibilityPermission> currentUserPermission,
                                                    final JsonObject response )
    {
        try
        {
            final Collection<DashboardEveryoneVisibility> dashboardEveryoneVisibilities = getDashboardEveryoneVisibility( orgId, dashboardVisibilityId );
            final JsonObject everyoneVisibilityJson = Json.object();

            if( dashboardEveryoneVisibilities.isEmpty() )
            {
                everyoneVisibilityJson.put( CRMDashboardKeys.IS_VISIBLE, false );
            }
            else
            {
                final DashboardEveryoneVisibility dashboardEveryoneVisibility = dashboardEveryoneVisibilities.stream()
                                                                                                             .findFirst()
                                                                                                             .get();
                final VisibilityPermission visibilityPermission = dashboardEveryoneVisibility.getVisibilityPermission();

                everyoneVisibilityJson.put( CRMDashboardKeys.IS_VISIBLE, true );
                everyoneVisibilityJson.put( CRMDashboardKeys.ACCESS, visibilityPermission.getId() );
                currentUserPermission.addAll( getUserAccessiblePermissions( visibilityPermission ) );
            }

            response.put( CRMDashboardKeys.EVERYONE, everyoneVisibilityJson );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                    "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * Retrieves the visibility settings for a specific dashboard within an organization
     *
     * @param orgId       The ID of the organization to which the dashboard belongs
     * @param dashboardId The ID of the dashboard whose visibility settings are to be retrieved
     * @return DashboardVisibility The visibility settings of the specified dashboard
     */
    private DashboardVisibility getDashboardVisibility( final Long orgId, final Long dashboardId )
    {
        try
        {
            final CRMViewConfigManager<DashboardVisibility> dashboardVisibilityManager = crmReportManagerAPI.getDashBoardVisibilityManager();
            final Criteria criteria = Criteria.eq( dashboardVisibilityManager.getTable()
                                                                             .getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ),
                                                   dashboardId );
            final Collection<DashboardVisibility> entityDashboardVisibilities = dashboardVisibilityManager.getAll( orgId, criteria );

            if( Objects.isNull( entityDashboardVisibilities ) || entityDashboardVisibilities.isEmpty() )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                     "i18n.analytics.config.dashboard.invalid.id" );
            }

            return entityDashboardVisibilities.stream()
                                              .findAny()
                                              .get();
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_ID,
                                 "i18n.analytics.config.dashboard.invalid.id" );
        }
    }

    /**
     * <p>
     * Retrieves the details of user with specific visibility permissions for a given dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @return A collection of {@link DashboardUserVisibility } that match the given criteria
     */
    private Collection<DashboardUserVisibility> getDashboardUserVisibility( final Long orgId, final Long dashboardVisibilityId )
    {
        try
        {
            final CRMViewConfigManager<DashboardUserVisibility> dashboardUserVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                            .getDashBoardUserVisibilityManager();
            final Criteria userVisibilityCriteria = Criteria.eq( dashboardUserVisibilityManager.getTable()
                                                                                               .getColumn( MCANADUSERVISBILITY.DASHBOARD_VISIBILITY_ID ),
                                                                 dashboardVisibilityId);

            return dashboardUserVisibilityManager.getAll( orgId, userVisibilityCriteria );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Retrieves the details of team with specific visibility permissions for a given dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @return A collection of {@link DashboardTeamVisibility } that match the given criteria
     */
    private Collection<DashboardTeamVisibility> getDashboardTeamVisibility( final Long orgId, final Long dashboardVisibilityId )
    {
        try
        {
            final CRMViewConfigManager<DashboardTeamVisibility> dashboardTeamVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                            .getDashBoardTeamVisibilityManager();
            final Criteria teamVisibilityCriteria = Criteria.eq( dashboardTeamVisibilityManager.getTable()
                                                                                               .getColumn( MCANADTEAMVISBILITY.DASHBOARD_VISIBILITY_ID ),
                                                                 dashboardVisibilityId);

            return dashboardTeamVisibilityManager.getAll( orgId, teamVisibilityCriteria );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Retrieves the details of territories with specific visibility permissions for a given dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @return A collection of {@link DashboardTerritoryVisibility } that match the given criteria
     */
    private Collection<DashboardTerritoryVisibility> getDashboardTerritoryVisibility( final Long orgId, final Long dashboardVisibilityId )
    {
        try
        {
            final CRMViewConfigManager<DashboardTerritoryVisibility> dashboardTerritoryVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                                      .getDashBoardTerritoryVisibilityManager();
            final Criteria territoryVisibilityCriteria = Criteria.eq( dashboardTerritoryVisibilityManager.getTable()
                                                                                                         .getColumn( MCANADTERRITORYVISBILITY.DASHBOARD_VISIBILITY_ID ),
                                                                      dashboardVisibilityId);

            return dashboardTerritoryVisibilityManager.getAll( orgId, territoryVisibilityCriteria );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Retrieves the details of everyone with specific visibility permissions for a given dashboard
     * </p>
     *
     * @param orgId                 Represents the organisation id
     * @param dashboardVisibilityId Represents the dashboard visibility configuration id
     * @return A collection of {@link DashboardEveryoneVisibility } that match the given criteria
     */
    private Collection<DashboardEveryoneVisibility> getDashboardEveryoneVisibility( final Long orgId, final Long dashboardVisibilityId )
    {
        try
        {
            final CRMViewConfigManager<DashboardEveryoneVisibility> dashboardEveryoneVisibilityManager = CRMReportManagerAPI.INSTANCE()
                                                                                                                    .getDashBoardEveryoneVisibilityManager();
            final Criteria everyoneVisibilityCriteria = Criteria.eq( dashboardEveryoneVisibilityManager.getTable()
                                                                                                       .getColumn( MCANADEVERYONEVISBILITY.DASHBOARD_VISIBILITY_ID ),
                                                                     dashboardVisibilityId );

            return dashboardEveryoneVisibilityManager.getAll( orgId, everyoneVisibilityCriteria );
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Deletes everyone visibility details for a specific dashboard
     * </p>
     *
     * @param orgId               Represents the organisation id
     * @param dashboardVisibility Represents the dashboard visibility configuration id
     */
    private void deleteEveryoneVisibilityDetails( final Long orgId, final Long dashboardVisibility )
    {
        final Collection<DashboardEveryoneVisibility> dashboardEveryoneVisibilities = getDashboardEveryoneVisibility( orgId, dashboardVisibility );

        if( Objects.nonNull( dashboardEveryoneVisibilities ) && !dashboardEveryoneVisibilities.isEmpty() )
        {
            CRMReportManagerAPI.INSTANCE()
                          .getDashBoardEveryoneVisibilityManager()
                          .delete( orgId, dashboardEveryoneVisibilities );
        }
    }

    /**
     * <p>
     * Deletes the user visibility details for a specific dashboard
     * </p>
     *
     * @param orgId               Represents the organisation id
     * @param dashboardVisibility Represents the dashboard visibility configuration id
     */
    private void deleteUserVisibilityDetails( final Long orgId, final Long dashboardVisibility )
    {
        final Collection<DashboardUserVisibility> dashboardUserVisibility = getDashboardUserVisibility( orgId, dashboardVisibility );

        if( Objects.nonNull( dashboardUserVisibility ) && !dashboardUserVisibility.isEmpty() )
        {
            CRMReportManagerAPI.INSTANCE()
                          .getDashBoardUserVisibilityManager()
                          .delete( orgId, dashboardUserVisibility );
        }
    }

    /**
     * <p>
     * Deletes the team visibility details for a specific dashboard
     * </p>
     *
     * @param orgId               Represents the organisation id
     * @param dashboardVisibility Represents the dashboard visibility configuration id
     */
    private void deleteTeamVisibilityDetails( final Long orgId, final Long dashboardVisibility )
    {
        final Collection<DashboardTeamVisibility> dashboardTeamVisibilities = getDashboardTeamVisibility( orgId, dashboardVisibility );

        if( Objects.nonNull( dashboardTeamVisibilities ) && !dashboardTeamVisibilities.isEmpty() )
        {
            CRMReportManagerAPI.INSTANCE()
                          .getDashBoardTeamVisibilityManager()
                          .delete( orgId, dashboardTeamVisibilities );
        }
    }

    /**
     * <p>
     * Deletes the territories visibility details for a specific dashboard
     * </p>
     *
     * @param orgId               Represents the organisation id
     * @param dashboardVisibility Represents the dashboard visibility configuration id
     */
    private void deleteTerritoryVisibilityDetails( final Long orgId, final Long dashboardVisibility )
    {
        final Collection<DashboardTerritoryVisibility> dashboardTerritoryVisibilities = getDashboardTerritoryVisibility( orgId, dashboardVisibility );

        if( Objects.nonNull( dashboardTerritoryVisibilities ) && !dashboardTerritoryVisibilities.isEmpty() )
        {
            CRMReportManagerAPI.INSTANCE()
                          .getDashBoardTerritoryVisibilityManager()
                          .delete( orgId, dashboardTerritoryVisibilities );
        }
    }

    /**
     * <p>
     * Retrieves the dashboard visibility details of the given user
     * </p>
     *
     * @param orgId       Represents the organisation id
     * @param userId      Represents the user id
     * @param dashboardId Represents the dashboard id configuration id
     * @return the {@link VisibilityPermission} of the user
     */
    private VisibilityPermission getUserVisibilityPermissions( final long orgId,
                                                               final long userId,
                                                               final Long dashboardId )
    {
        try
        {
            final CRMReportManagerAPI crmManager = CRMReportManagerAPI.INSTANCE();
            final CRMViewConfigManager<DashboardVisibility> dashboardVisibilityManager = crmManager.getDashBoardVisibilityManager();
            final Criteria criteria = Criteria.eq( dashboardVisibilityManager.getTable()
                                                                             .getColumn( OMCANADASHBOARDVISBILITY.DASH_ID ), dashboardId );
            final Collection<DashboardVisibility> dashboardVisibilities = dashboardVisibilityManager.getAll( orgId, criteria );
            final Collection<VisibilityPermission> visibilityPermission = new ArrayList<>();

            if( Objects.nonNull( dashboardVisibilities ) && !dashboardVisibilities.isEmpty() )
            {
                final DashboardVisibility dashboardVisibility = dashboardVisibilities.stream()
                                                                                     .findAny()
                                                                                     .get();
                final Long dashboardVisibilityId = dashboardVisibility.id();

                setDashboardUserVisibility( orgId, userId, dashboardVisibilityId, visibilityPermission );
                setDashboardTeamVisibility( orgId, userId, dashboardVisibilityId, visibilityPermission );
                setDashboardTerritoryVisibility( orgId, userId, dashboardVisibilityId, visibilityPermission );
                setDashboardEveryoneVisibility( orgId, dashboardVisibilityId, visibilityPermission );
            }

            return visibilityPermission.isEmpty() ? null : visibilityPermission.stream()
                                                                               .findFirst()
                                                                               .get();
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Sets the dashboard team user visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param orgId                     Represents the organisation id
     * @param userId                    Represents the user id
     * @param teamVisibilityPermissions Represents the team user dashboard visibility permission details
     * @param userVisibilityPermissions Represents the user and its visibility permission details
     */
    private void setDashboardTeamUserVisibility( final Long orgId,
                                                 final Long userId,
                                                 final Map<Long, VisibilityPermission> teamVisibilityPermissions,
                                                 final Collection<VisibilityPermission> userVisibilityPermissions )
    {
        try
        {
            final Collection<Team> teams = teamManager.getTeams( orgId, teamVisibilityPermissions.keySet(), false );

            if( !teams.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( teams,
                                           team ->
                                                 {
                                                     final Collection<TeamUser> teamUsers = team.getUsers();

                                                     if( Objects.nonNull( teamUsers ) && !teamUsers.isEmpty() )
                                                     {
                                                         CodeBlockExecutor.forEachSafely( teamUsers,
                                                                                teamUser ->
                                                                                          {
                                                                                              final CrmUser crmUser = teamUser.getCrmUser();

                                                                                              if( Objects.nonNull( crmUser ) )
                                                                                              {
                                                                                                  final Long id = crmUser.id();

                                                                                                  if( Objects.equals( userId, id ) )
                                                                                                  {
                                                                                                      final VisibilityPermission teamVisibility = teamVisibilityPermissions.get( team.id() );

                                                                                                      if( userVisibilityPermissions.isEmpty() )
                                                                                                      {
                                                                                                          userVisibilityPermissions.add( teamVisibility );
                                                                                                      }
                                                                                                      else
                                                                                                      {
                                                                                                          final VisibilityPermission existingVisibility = userVisibilityPermissions.stream()
                                                                                                                                                                                   .findFirst()
                                                                                                                                                                                   .get();

                                                                                                          if( existingVisibility.getId() < teamVisibility.getId() )
                                                                                                          {
                                                                                                              userVisibilityPermissions.add( teamVisibility );
                                                                                                          }
                                                                                                      }
                                                                                                  }
                                                                                              }
                                                                                          } );
                                                     }
                                                 } );

            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Retrieves the dashboard visibility details
     * </p>
     *
     * @param visibilityPermission Represents the visibility permission configuration
     * @return the {@link VisibilityPermission}
     */
    private Collection<VisibilityPermission> getUserAccessiblePermissions( final VisibilityPermission visibilityPermission )
    {
        final Collection<VisibilityPermission> accessiblePermissions = new HashSet<>();

        switch ( visibilityPermission )
        {
            case CAN_VIEW ->
            {
                accessiblePermissions.add( VisibilityPermission.CAN_VIEW );
            }
            case CAN_EDIT ->
            {
                accessiblePermissions.add( VisibilityPermission.CAN_VIEW );
                accessiblePermissions.add( VisibilityPermission.CAN_EDIT );
            }
            case CAN_MANAGE ->
            {
                accessiblePermissions.add( VisibilityPermission.CAN_VIEW );
                accessiblePermissions.add( VisibilityPermission.CAN_EDIT );
                accessiblePermissions.add( VisibilityPermission.CAN_MANAGE );
            }
            case OWNER ->
            {
                accessiblePermissions.add( VisibilityPermission.CAN_VIEW );
                accessiblePermissions.add( VisibilityPermission.CAN_EDIT );
                accessiblePermissions.add( VisibilityPermission.CAN_MANAGE );
                accessiblePermissions.add( VisibilityPermission.OWNER );
            }
        }

        return accessiblePermissions;
    }

    /**
     * <p>
     * Sets the dashboard user visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param orgId                     Represents the organisation id
     * @param userId                    Represents the user id
     * @param dashboardVisibilityId     Represents the dashboard visibility configuration id
     * @param userVisibilityPermissions Represents the user and its visibility permission details
     */
    private void setDashboardUserVisibility( final Long orgId,
                                             final Long userId,
                                             final Long dashboardVisibilityId,
                                             final Collection<VisibilityPermission> userVisibilityPermissions )
    {
        try
        {
            final Collection<DashboardUserVisibility> dashboardUserVisibilities = getDashboardUserVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardUserVisibilities ) && !dashboardUserVisibilities.isEmpty() )
            {
                CodeBlockExecutor.forEachSafely( dashboardUserVisibilities,
                             dashboardUserVisibility ->
                                                 {
                                                     if( Objects.equals( userId, dashboardUserVisibility.getUserId() ) )
                                                     {
                                                         userVisibilityPermissions.add( dashboardUserVisibility.getVisibilityPermission() );
                                                     }
                                                 } );
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                        "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Sets the dashboard team visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param orgId                     Represents an organisation id
     * @param userId                    Represents the user id
     * @param dashboardVisibilityId     Represents the dashboard visibility configuration id
     * @param userVisibilityPermissions Represents the user and its visibility permission details
     */
    private void setDashboardTeamVisibility( final Long orgId,
                                             final Long userId,
                                             final Long dashboardVisibilityId,
                                             final Collection<VisibilityPermission> userVisibilityPermissions )
    {
        try
        {
            final Collection<DashboardTeamVisibility> dashboardTeamVisibilities = getDashboardTeamVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardTeamVisibilities ) && !dashboardTeamVisibilities.isEmpty() )
            {
                final Map<Long, VisibilityPermission> teamVisibilityPermissions = new HashMap<>();

                CodeBlockExecutor.forEachSafely( dashboardTeamVisibilities,
                            dashboardTeamVisibility ->
                                                 {
                                                     teamVisibilityPermissions.put( dashboardTeamVisibility.getTeamId(),
                                                                                    dashboardTeamVisibility.getVisibilityPermission() );
                                                 } );

                if( !teamVisibilityPermissions.isEmpty() )
                {
                    setDashboardTeamUserVisibility( orgId, userId, teamVisibilityPermissions, userVisibilityPermissions );
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Sets the dashboard territory visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param orgId                     Represents the organisation id
     * @param userId                    Represents an user id
     * @param dashboardVisibilityId     Represents the dashboard visibility configuration id
     * @param userVisibilityPermissions Represents the user and its visibility permission details
     */
    private void setDashboardTerritoryVisibility( final Long orgId,
                                                  final Long userId,
                                                  final Long dashboardVisibilityId,
                                                  final Collection<VisibilityPermission> userVisibilityPermissions )
    {
        try
        {
            final Collection<DashboardTerritoryVisibility> dashboardTerritoryVisibilities = getDashboardTerritoryVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardTerritoryVisibilities ) && !dashboardTerritoryVisibilities.isEmpty() )
            {
                final Map<Long, VisibilityPermission> territoryVisibilityPermissions = new HashMap<>();
                final Map<Long, VisibilityPermission> teamVisibilityPermissions = new HashMap<>();
                final Map<Long, VisibilityPermission> territoryUsersVisibilityPermissions = new HashMap<>();

                CodeBlockExecutor.forEachSafely( dashboardTerritoryVisibilities,
                         dashboardTerritoryVisibility ->
                                                {
                                                    territoryVisibilityPermissions.put( dashboardTerritoryVisibility.getTerritoryId(), dashboardTerritoryVisibility.getVisibilityPermission() );
                                                } );

                if( !territoryVisibilityPermissions.isEmpty() )
                {
                    final Map<Long, Collection<CrmUser>> crmUsers = new HashMap<>();
                    final Map<Long, Collection<Team>> teams = new HashMap<>();
                    final Collection<Territory> territories = territoryManager.getTerritories( orgId, territoryVisibilityPermissions.keySet(), false );

                    CodeBlockExecutor.forEachSafely( territories,
                                             territory ->
                                                     {
                                                         final Long territoryId = territory.id();

                                                         crmUsers.put( territoryId, territory.getCrmUsers() );
                                                         teams.put( territoryId, territory.getTeams() );
                                                     } );

                    if( !crmUsers.isEmpty() )
                    {
                        CodeBlockExecutor.forEachSafely( crmUsers.entrySet(),
                                 crmUser ->
                                                         {
                                                             final Long territoryId = crmUser.getKey();
                                                             final Collection<CrmUser> users = crmUser.getValue();

                                                             if( Objects.nonNull( users ) && !users.isEmpty() )
                                                             {
                                                                 CodeBlockExecutor.forEachSafely( users,
                                                                                         user ->
                                                                                         {
                                                                                             territoryUsersVisibilityPermissions.put( user.id(),
                                                                                                                                      territoryVisibilityPermissions.get( territoryId ) );
                                                                                         } );
                                                             }
                                } );

                        if( !territoryUsersVisibilityPermissions.isEmpty() )
                        {
                            CodeBlockExecutor.forEachSafely( territoryUsersVisibilityPermissions.keySet(),
                                                        crmUserId ->
                                                        {
                                                            if( Objects.equals( userId, crmUserId ) )
                                                            {
                                                                final VisibilityPermission territoryVisibility = territoryUsersVisibilityPermissions.get( crmUserId );

                                                                if( userVisibilityPermissions.isEmpty() )
                                                                {
                                                                    userVisibilityPermissions.add( territoryVisibility );
                                                                }
                                                                else
                                                                {
                                                                    final VisibilityPermission existingVisibility = userVisibilityPermissions.stream()
                                                                                                                                             .findFirst()
                                                                                                                                             .get();

                                                                    if( existingVisibility.getId() < territoryVisibility.getId() )
                                                                    {
                                                                        userVisibilityPermissions.add( territoryVisibility );
                                                                    }
                                                                }
                                                            }
                                                        } );
                        }
                    }

                    if( !teams.isEmpty() )
                    {
                        CodeBlockExecutor.forEachSafely( teams.entrySet(),
                                 team ->
                                                         {
                                                             final Long territoryId = team.getKey();
                                                             final Collection<Team> territoryTeams = team.getValue();

                                                             if( Objects.nonNull( territoryTeams ) && !territoryTeams.isEmpty() )
                                                             {
                                                                 CodeBlockExecutor.forEachSafely( territoryTeams,
                                                                                            territoryTeam ->
                                                                                            {
                                                                                                teamVisibilityPermissions.put( territoryTeam.id(),
                                                                                                                               territoryVisibilityPermissions.get( territoryId ) );
                                                                                            } );
                                                             }
                                                         } );

                        if( !teamVisibilityPermissions.isEmpty() )
                        {
                            setDashboardTeamUserVisibility( orgId, userId, teamVisibilityPermissions, userVisibilityPermissions );
                        }
                    }
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                         "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }

    /**
     * <p>
     * Sets the dashboard visibility permissions for a given dashboard visibility id and organization
     * </p>
     *
     * @param orgId                     Represents the organisation id
     * @param dashboardVisibilityId     Represents the dashboard visibility configuration id
     * @param userVisibilityPermissions Represents the user and its visibility permission details
     */
    private void setDashboardEveryoneVisibility( final Long orgId,
                                                 final Long dashboardVisibilityId,
                                                 final Collection<VisibilityPermission> userVisibilityPermissions )
    {
        try
        {
            final Collection<DashboardEveryoneVisibility> dashboardEveryoneVisibilities = getDashboardEveryoneVisibility( orgId, dashboardVisibilityId );

            if( Objects.nonNull( dashboardEveryoneVisibilities ) && !dashboardEveryoneVisibilities.isEmpty() )
            {
                final DashboardEveryoneVisibility dashboardEveryoneVisibility = dashboardEveryoneVisibilities.stream()
                                                                                                             .findFirst()
                                                                                                             .get();
                final VisibilityPermission everyoneVisibility = dashboardEveryoneVisibility.getVisibilityPermission();

                if( userVisibilityPermissions.isEmpty() )
                {
                    userVisibilityPermissions.add( everyoneVisibility );
                }
                else
                {
                    final VisibilityPermission existingVisibility = userVisibilityPermissions.stream()
                                                                                             .findFirst()
                                                                                             .get();

                    if( existingVisibility.getId() < everyoneVisibility.getId() )
                    {
                        userVisibilityPermissions.add( everyoneVisibility );
                    }
                }
            }
        }
        catch( final Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                        "i18n.crm.analytics.visibility.permission.invalid" );
        }
    }
}

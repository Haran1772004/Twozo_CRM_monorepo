package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.iam.api.manager.impl.TeamManagerImpl;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.internal.manager.IamConfigManger;
import com.tlc.crm.common.iam.internal.manager.UserLinkManager;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Performs the functionality for {@link TeamManager} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface TeamManager extends IamConfigManger<Team>, UserLinkManager<Team> {

    /**
     * <p>
     * Gets the team manger instance
     * </p>
     *
     * @return The {@link TeamManagerImpl}
     */
    static TeamManager getInstance() {
        return TeamManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the user team table
     * </p>
     *
     * @return The user team {@link Table}
     */
    Table getUserTeamTable();

    /**
     * <p>
     * Checks if any one of the teams are having one of the given team names excluding the given ids
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param teamIds Contains the collection of team ids
     * @param names   Contains the collection of team names
     * @return If any team is exists it will return true, otherwise it will return false
     */
    boolean checkTeamsAreExists(final Long orgId, final Collection<Long> teamIds, final Collection<String> names);

    /**
     * <p>
     * Retrieves the team using id with start and limit criteria.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param teamId         Represents the team id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The {@link Team} details
     */
    Team get(final Long orgId, final Long teamId, final int userStartValue, final int userLimit);

    /**
     * <p>
     * Retrieves the total number of users.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param teamId Represents the team id
     * @return The total number of users
     */
    long getUsersCount(final Long orgId, final Long teamId);

    /**
     * <p>
     * Retrieves the teams by given organization id with start and limit criteria
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of team to be fetched
     * @param limit Represents the maximum number of team to be fetched
     * @return The collection of {@link Team} details
     */
    Collection<Team> get(final Long orgId, final int start, final int limit);

    /**
     * <p>
     * Checks whether any team is exists with one of the given names.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of team names
     * @return If any team is exists it will return true, otherwise it will return false
     */
    boolean checkTeamsAreExists(final Long orgId, final Collection<String> names);

    /**
     * <p>
     * Retrieves the users by given team id with start and limit criteria
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param teamId         Represents the team id
     * @param fetchManagers  A flag indicates whether there is a need to retrieve users assigned as managers or not.
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The Collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsers(final Long orgId, final Long teamId, final boolean fetchManagers, final int userStartValue, final int userLimit);

    /**
     * Gets the total number of managers of a team.
     *
     * @param orgId  Represents the organization id
     * @param teamId Represents the team id.
     * @return The total number of managers of a team.
     */
    long getTeamManagersCount(final long orgId, final long teamId);

    /**
     * Gets the total number of manager ids of a team.
     *
     * @param orgId  Represents the organization id
     * @param teamId Represents the team id.
     * @return The total number of manager ids of a team.
     */
    Collection<Long> getTeamManagerIds(final long orgId, final long teamId);

    /**
     * Designates the manager role for the team user.
     *
     * @param orgId           Refers the organization id.
     * @param teamId          Refers the team id.
     * @param userId          Refers the user id.
     * @param assignAsManager A flag indicates whether the user linkUsers as manager or not.
     * @return The updated status.
     */
    boolean designateUserAsManager(final long orgId, final long teamId, final long userId, final boolean assignAsManager);

    /**
     * Checks whether the user is a member of the team or not.
     *
     * @param orgId  Refers the organization id.
     * @param teamId Refers the team id.
     * @param userId Refers the user id.
     * @return true, if the user is a member of the team, otherwise false.
     */
    boolean isUserPartOfTeam(final long orgId, final long teamId, final long userId);

    /**
     * Checks whether the users are member of the team or not.
     *
     * @param orgId   Refers the organization id.
     * @param teamId  Refers the team id.
     * @param userIds Refers the set of user ids.
     * @return True, if the user is a member of the team, otherwise false.
     */
    boolean areUsersPartOfTeam(final long orgId, final long teamId, final Set<Long> userIds);

    /**
     * <p>
     * Fetches the team dropdown values based on the specified start and limit conditions.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and team the team to be fetched
     * @param criteria    Represents the criteria for role filtering
     * @return The collection of {@link Team} details
     */
    Collection<Team> getTeamDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria);

    /**
     * <p>
     * Fetches the user ids for team members within the given organization.
     *
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param teamIds Represents the collection of team ids
     * @return Collection of {@link Long} user ids related to the teams
     */
    Collection<Long> getTeamUserIds(final Long orgId, final Collection<Long> teamIds);

    /**
     * <p>
     * Fetches the user ids for team members within the given organization. The team user ids are fetched by
     * filtering out the users present in excluded users collection.
     *
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param teamIds       Represents the collection of team ids
     * @param excludedUsers Represents the collection of user ids to be filtered
     * @return Collection of {@link Long} user ids related to the teams
     */
    Collection<Long> getTeamUserIds(final Long orgId, final Collection<Long> teamIds, final Collection<Long> excludedUsers);

    /**
     * <p>
     * Fetches the collection of teammate's user ids for the given user id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return Collection of user ids.
     */
    Collection<Long> getTeamMateIds(final Long orgId, final Long userId);

    /**
     * <p>
     * Checks if there are any existing associations between the specified organization id and team ids
     * </p>
     *
     * @param orgId   Represents the organization id
     * @param teamIds Represents the collection of team ids
     * @return True if team ids exist, otherwise false
     */
    boolean exists(final long orgId, final Collection<Long> teamIds);

    /**
     * <p>
     * Fetches the {@link Collection} of {@link Team} within the given organization
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param teamIds   Represents the {@link Collection} of {@link Team} ids
     * @param isPartial Represents whether partial data required or not
     * @return The {@link Collection} of {@link Team}
     */
    Collection<Team> getTeams(final long orgId, final Collection<Long> teamIds, final boolean isPartial);

    /**
     * <p>
     * Gets the team ids for the given user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id.
     * @return the team ids
     */
    Collection<Long> getUserTeamIds(final long orgId, final long userId);

    /**
     * <p>
     * Fetches the {@link Team} within the given organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param teamId Represents the {@link Team}
     * @return the {@link Team}
     */
    Team getTeam(final long orgId, final Long teamId);

    /**
     * <p>
     * Gets the collection of {@link Team} based on the given criteria
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the {@link SearchFilter}
     * @return The collection of {@link Team}
     */
    Collection<Team> getTeams(final long orgId, final SearchFilter searchFilter);

    /**
     * <p>
     * Gets the total count of {@link Team} based on the given criteria
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the {@link SearchFilter}
     * @return The total team count
     */
    long getTeamsCount(final long orgId, final SearchFilter searchFilter);

    /**
     * <p>
     * Fetches the team dropdown values based on the specified start and limit conditions
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param requiredUserCount A flag indicating whether the territory should include the user count
     * @param limitClause       Represents the start and team the team to be fetched
     * @param criteria          Represents the criteria for role filtering
     * @return The collection of {@link Team} details
     */
    Collection<Team> getTeamDropDownList( final Long orgId,
                                          final boolean requiredUserCount,
                                          final LimitClause limitClause,
                                          final Criteria criteria );

    /**
     * <p>
     * Returns the table representing the team entities.
     * </p>
     *
     * @return The {@link Table} representing the team table
     */
    Table getTable();

    /**
     * Retrieves a collection of {@link Team} entities for the specified organization,
     * optionally filtered by the provided search criteria.
     *
     * @param orgId        The unique identifier of the organization whose teams are to be retrieved.
     * @param searchFilter An optional {@link SearchFilter} containing filtering and pagination criteria;
     *                     may be {@code null} to fetch all teams.
     * @return A collection of {@link Team} objects matching the specified organization and filter criteria.
     */
    Collection<Team> getTeamList(final long orgId, final SearchFilter searchFilter);
}
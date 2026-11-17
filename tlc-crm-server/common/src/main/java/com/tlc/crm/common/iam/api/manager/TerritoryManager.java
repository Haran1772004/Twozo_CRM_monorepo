package com.tlc.crm.common.iam.api.manager;

import com.tlc.crm.common.iam.api.manager.impl.TerritoryManagerImpl;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.internal.manager.IamConfigManger;
import com.tlc.crm.common.iam.internal.manager.UserLinkManager;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Performs the functionality for {@link TerritoryManager} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface TerritoryManager extends IamConfigManger<Territory>, UserLinkManager<Territory> {

    /**
     * <p>
     * Gets the territory manager instance
     * </p>
     *
     * @return The {@link TerritoryManagerImpl}
     */
    static TerritoryManager getInstance() {
        return TerritoryManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the user territory table
     * </p>
     *
     * @return The user territory {@link Table}
     */
    Table getUserTerritoryTable();

    /**
     * <p>
     * Checks if any one of the territory are having one of the given territory names excluding the given ids
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of territory ids
     * @param names Contains the collection of territory names
     * @return If any territory is exists it will return true, otherwise it will return false
     */
    boolean checkTerritoriesAreExists(final Long orgId, final Collection<Long> ids, final Collection<String> names);

    /**
     * <p>
     * Retrieves the territory by given id with start and limit criteria
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param territoryId    Represents the territory id
     * @param userStartValue Represents the starting index of users to be fetched
     * @param userLimit      Represents the maximum number of users to be fetched
     * @return The {@link Territory} details
     */
    Territory get(final Long orgId, final Long territoryId, final int userStartValue, final int userLimit);

    /**
     * <p>
     * Retrieves the territories by given organization id with start and limit criteria
     * </p>
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of territory to be fetched
     * @param limit Represents the maximum number of territory to be fetched
     * @return The collection of {@link Territory} details
     */
    Collection<Territory> get(final Long orgId, final int start, final int limit);

    /**
     * <p>
     * Retrieves the total number of users
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the territory id
     * @return The total number of users
     */
    long getUsersCount(final Long orgId, final Long id);

    /**
     * <p>
     * Checks whether any territory is exists with one of the given names.
     * </p>
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of territory names
     * @return If any territory is exists it will return true, otherwise it will return false
     */
    boolean checkTerritoriesAreExists(final Long orgId, final Collection<String> names);

    /**
     * <p>
     * Retrieves the users by given territory id with start and limit criteria
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param territoryId    Represents the territory id
     * @param userStartValue Represents the starting index of users to be fetched
     * @param userLimit      Represents the maximum number of users to be fetched
     * @return The Collection of {@link CrmUser} details
     */
    Collection<CrmUser> getUsers(final Long orgId, final Long territoryId, final int userStartValue, final int userLimit);

    /**
     * <p>
     * Fetches the territory dropdown values based on the specified start and limit conditions.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and limit the territory to be fetched
     * @param criteria    Represents the criteria for territory filtering
     * @return The collection of {@link Territory} details
     */
    Collection<Territory> getTerritoryDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria);

    /**
     * <p>
     * Fetches the user ids for territory members within the given organization.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param territoryIds Represents the collection of territory ids
     * @return Collection of {@link Long} user ids related to the territories
     */
    Collection<Long> getTerritoryUserIds(final Long orgId, final Collection<Long> territoryIds);

    /**
     * <p>
     * Fetches the {@link Territory} of the given user.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return {@link Territory} of the given user.
     */
    Collection<Long> getUserTerritoryIds(final Long orgId, final Long userId);

    /**
     * <p>
     * Fetches the {@link Collection} of {@link Territory} within the given organization
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param territoryIds Represents the {@link Collection} of {@link Territory} ids
     * @param isPartial    Represents whether partial data required or not
     * @return the {@link Collection} of {@link Territory}
     */
    Collection<Territory> getTerritories(final long orgId, final Collection<Long> territoryIds, final boolean isPartial);

    /**
     * <p>
     * Fetches the collection of territory mate's user ids for the given user id.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return Collection of user ids.
     */
    Collection<Long> getTerritoryMateIds(final Long orgId, final Long userId);

    /**
     * Gets the {@link Territory} by the given name and organization ID
     *
     * @param orgId         Represents the organization ID
     * @param territoryName Name of the territory
     * @return the {@link Territory}
     */
    Optional<Territory> getTerritoryByName(final long orgId, final String territoryName);

    /**
     * <p>
     * Fetches the territory dropdown values based on the specified start and limit conditions
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param requiredUserCount A flag indicating whether the territory should include the user count
     * @param limitClause       Represents the start and limit the territory to be fetched
     * @param criteria          Represents the criteria for territory filtering
     * @return The collection of {@link Territory} details
     */
    Collection<Territory> getTerritoryDropDownList(final Long orgId,
                                                   final boolean requiredUserCount,
                                                   final LimitClause limitClause,
                                                   final Criteria criteria);

    /**
     * <p>
     * Gets the total territory count based on the given criteria
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the criteria for territory filtering
     * @return The total territory count
     */
    long getTerritoriesCount(final Long orgId, final SearchFilter searchFilter);

    /**
     * <p>
     * Returns the table representing the team entities.
     * </p>
     *
     * @return The {@link Table} representing the team table
     */
    Table getTable();

    /**
     * Checks whether the users are part of a territory or not.
     *
     * @param orgId       Represents an organization id.
     * @param territoryId Represents the territory id.
     * @param userIds     Refers the user ids.
     * @return true, if users are all part of territory, otherwise false.
     */
    boolean areUsersPartOfTerritory(final long orgId, final long territoryId, final Collection<Long> userIds);

    /**
     * Retrieves a collection of {@link Territory} entities for the specified organization,
     * optionally filtered by the provided search criteria.
     *
     * @param orgId        The unique identifier of the organization whose territories are to be retrieved.
     * @param searchFilter An optional {@link SearchFilter} containing filtering and pagination criteria;
     *                     may be {@code null} to fetch all teams.
     * @return A collection of {@link Territory} objects matching the specified organization and filter criteria.
     */
    Collection<Territory> getTerritoryList(final long orgId, final SearchFilter searchFilter);

    /**
     * <p>
     * Gets the territory name by given territory id and organization id
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param territoryId  Represents the territory id
     * @return The territory name if exists, otherwise returns null
     */
    String getName(final long orgId, final long territoryId);
}

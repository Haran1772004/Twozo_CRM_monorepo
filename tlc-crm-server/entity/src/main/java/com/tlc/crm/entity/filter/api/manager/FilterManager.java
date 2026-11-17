package com.tlc.crm.entity.filter.api.manager;

import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.filter.api.manager.impl.FilterManagerImpl;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.sql.common.api.DataContainer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provides the {@link Filter} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface FilterManager extends ConfigManager<Filter> {

    static FilterManager getInstance() {
        return FilterManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the collection of {@link Filter} based on organisation id.
     * </p>
     *
     * @param orgId       Represents the organisation id.
     * @param userId      The ID of the user requesting the filters.
     * @param sourceId    Represents the source id.
     * @param limit       Holds the limitation details.
     * @param category    The category of the filters to be retrieved.
     * @return collection of {@link Filter}.
     */
    Collection<Filter> getOrgFilters(final Long orgId, final Long userId, final Long sourceId, final DropdownLimit limit,
                                     final Filter.Category category);

    /**
     * <p>
     * Gets a collection of sales owner {@link Filter} objects associated with a set of users in the given organization.
     * </p>
     *
     * @param orgId         Represents an organisation id.
     * @param currentUserId Represents the current user id
     * @param sourceId      Represents the source id.
     * @param userIds       The collection of user IDs to filter by.
     * @return collection of {@link Filter}.
     */
    Collection<Filter> getSalesOwnerFilters(final Long orgId, final Long currentUserId, final Long sourceId, final Collection<Long> userIds);

    /**
     * <p>
     * Updates the favourite filters.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param filterId For modify favourite list.
     * @param isAdd    A flag that represent the filter is favourite or not.
     */
    void updateFavouriteFilter(final Long orgId, final Long filterId, final Long userId, final boolean isAdd);

    /**
     * <p>
     * Fetches the source filters of the given provider ids.
     * </p>
     *
     * @return Map of provider id and their collection of {@link Filter}.
     */
    Map<Long, Collection<Filter>> getDefaultFilters();

    /**
     * <p>
     * Fetches the permissions of the given filter ids.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param userId    Represents the user id.
     * @param filterIds Holds the filter ids.
     * @return Map of {@link Filter.Permissions}.
     */
    Map<Long, Filter.Permissions> getPermissions(final Long orgId, final Long userId, final Collection<Long> filterIds);

    /**
     * <p>
     * Fetches the visibility of the given filter ids.
     * </p>
     *
     * @param orgId     Represents the organisation id.
     * @param filterIds Holds the filter ids.
     * @return Map of {@link FilterVisibility}.
     */
    Map<Long, FilterVisibility> getVisibility(final Long orgId, final Collection<Long> filterIds);

    /**
     * <p>
     * Fetches full criteria details for the given criteria ids.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param criteriaIds Holds the criteria ids.
     * @return Collection of {@link FilterCriteria}.
     */
    Collection<FilterCriteria> getCriteria(final Long orgId, final Collection<Long> criteriaIds);

    /**
     * <p>
     * Fetches criteria rows and child rows required to update criteria details.
     * </p>
     *
     * @param orgId       Represents the organization id.
     * @param criteriaIds Holds the criteria ids.
     * @return {@link DataContainer} that holds criteria rows and child rows.
     */
    DataContainer getCriteriaContainer(final Long orgId, final Collection<Long> criteriaIds);

    /**
     * <p>
     * Loads standalone criteria details into the given target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filterCriteria  Holds criteria attributes.
     * @param sourceContainer Holds existing criteria rows.
     * @param targetContainer Holds updated criteria rows.
     */
    void loadCriteriaIntoContainer(final boolean isCreateEvent, final FilterCriteria filterCriteria,
                                   final DataContainer sourceContainer, final DataContainer targetContainer);

    /**
     * <p>
     * Updates the filter that is currently active.
     * </p>
     *
     * @param orgId      Represents the organisation id.
     * @param userId     Represents the user id.
     * @param providerId Represents the provider id.
     * @param filterId   Represents the filter id.
     */
    void updateRecentlyAppliedFilter(final Long orgId, final Long userId, final Long providerId, final Long filterId);

    /**
     * <p>
     * Removes the sales owner filter from the DB.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param userIds Represents the user ids.
     */
    void deleteSalesOwnerFilter(final Long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Stores the active filters in recently applied filter table
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param filterIds Represents the filter ids
     */
    void storeActiveFilters(final long orgId, final long userId, final Map<Long, Long> filterIds);

    /**
     * <p>
     * Gets the filter id by using provider id and aliasName
     * </p>
     *
     * @param orgId       Represents the organisation Id
     * @param providerId  Represents the provider Id
     * @param aliasName   Represents the filter alias name
     * @return The filter id
     */
    Long getFilterId(final Long orgId, final Long providerId, final String aliasName);

    /**
     * <p>
     * Retrieves the user IDs of recently applied sales owner filter
     * </p>
     *
     * @param orgId Represents the organisation ID
     * @return optional of user id
     */
    Optional<Long> getRecentlyAppliedFilterUserId(final Long orgId, final Long userId, final Long sourceId);

    /**
     * <p>
     * Deletes criteria rows and their child rows from the database.
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param criteriaIds Represents the collection of criteria IDs to delete
     */
    void deleteCriteria(final Long orgId, final Collection<Long> criteriaIds);
}

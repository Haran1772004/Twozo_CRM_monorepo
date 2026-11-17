package com.tlc.crm.notifications.marketplace.api.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.notifications.marketplace.api.manager.internal.MCubeActionManagerImpl;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;

import java.util.Collection;
import java.util.Optional;

/**
 * Manager class for handling MCube-related marketplace applications.
 *
 * @author Sathish
 * @version 1.0
 */
public interface MCubeActionManager extends TelephoneActionManager {

    /**
     * Gets the instance of {@link MCubeActionManager}.
     *
     * @return The {@link MCubeActionManager} instance.
     */
    static MCubeActionManager getInstance() {
        return MCubeActionManagerImpl.getInstance();
    }

    /**
     * Updates MCube user configuration for a given organization.
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace application ID
     * @param crmMCubeUsers    Collection of {@link CrmMCubeUser}
     */
    void updateUser(final long orgId, final long marketplaceAppId, final Collection<CrmMCubeUser> crmMCubeUsers);

    /**
     * Removes an MCube user from configuration.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     */
    void removeUser(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * Removes MCube users from configuration.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userIds          Collection of user ids
     */
    void removeUser(final long orgId, final long marketplaceAppId, final Collection<Long> userIds);

    /**
     * Retrieves a specific MCube user.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     * @return an Optional containing the {@link CrmMCubeUser} if found
     */
    Optional<CrmMCubeUser> getUser(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * Fetches MCube users with pagination and sorting.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param limitConfig      pagination configuration
     * @param sortConfig       sorting configuration
     * @return A collection of {@link CrmMCubeUser}
     */
    Collection<CrmMCubeUser> getUsers(final long orgId, final long marketplaceAppId,
                                      final LimitConfig limitConfig, final SortConfig sortConfig);

    /**
     * Fetches the total number of MCube users.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @return The total number of users
     */
    long getUserTotalRecords(final long orgId, final long marketplaceAppId);

    /**
     * Checks whether a user exists.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     * @return true if the user exists
     */
    boolean isUserExist(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * Checks whether authentication data already exists with the given authentication token.
     *
     * @param authToken Represents the MCube authentication token
     * @return true if exists
     */
    boolean isAuthTokenExists(final String authToken, final long marketplaceAppId);

    /**
     * Fetches MCube users by user IDs.
     *
     * @param orgId   Represents an organization ID
     * @param userIds Collection of user ids
     * @return The collection of {@link CrmMCubeUser}
     */
    Collection<CrmMCubeUser> getUsers(final long orgId, final Collection<Long> userIds);

    /**
     * Fetches MCube users by user IDs.
     *
     * @param orgId   Represents an organization ID
     * @param userId  Represents a user ID
     * @return The collection of {@link CrmMCubeUser}
     */
    Optional<CrmMCubeUser> getUser(final long orgId, final long userId);

    /**
     * Gets the user ids for the given org id.
     *
     * @param orgId            Represents an organization id
     * @param marketplaceAppId Represents the marketplace application ID
     * @return The collection of user ids
     */
    Collection<Long> getUserIds(final long orgId, final long marketplaceAppId);
}


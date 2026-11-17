package com.tlc.crm.notifications.marketplace.api.manager;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.common.models.SortConfig;
import com.tlc.crm.notifications.marketplace.api.manager.internal.TeleCMIActionManagerImpl;
import com.tlc.crm.notifications.marketplace.api.model.CrmTeleCMIUser;

import java.util.Collection;
import java.util.Optional;

/**
 * Manager class for handling telecmi-related marketplace applications
 *
 * @author kishore J
 * @version 1.0
 */
public interface TeleCMIActionManager extends TelephoneActionManager {

    /**
     * Gets the instance of {@link TeleCMIActionManager}.
     *
     * @return The {@link  TeleCMIActionManager} instance.
     */
    static TeleCMIActionManager getInstance() {
        return TeleCMIActionManagerImpl.getInstance();
    }

    /**
     * <p>
     * Updates {@link CrmTeleCMIUser} configuration for a given organization
     * </p>
     *
     * @param orgId            Represents an org id
     * @param marketplaceAppId Represents the marketplace application ID
     * @param crmTeleCMIUsers  Which contains the collection {@link CrmTeleCMIUser}
     */
    void updateUser(final long orgId, final long marketplaceAppId, final Collection<CrmTeleCMIUser> crmTeleCMIUsers);

    /**
     * <p>
     * Removes a TeleCMI user from configuration for a given organization
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     */
    void removeUser(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * <p>
     * Removes a TeleCMI user from configuration for a given organization
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userIds          Which contains the collection of user ids
     */
    void removeUser(final long orgId, final long marketplaceAppId, final Collection<Long> userIds);

    /**
     * Retrieves a specific TeleCMI user for the given organization, marketplace app, and user ID.
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     * @return an {@link Optional} containing the {@link CrmTeleCMIUser} if found, otherwise empty
     */
    Optional<CrmTeleCMIUser> getUser(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * <p>
     * Fetches the maximum extension number assigned to any user for the given organization and marketplace application
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @return The highest extension number assigned among users
     */
    int getUserMaxExtension(final long orgId, final long marketplaceAppId);

    /**
     * <p>
     * Fetches a collection of TeleCMI users for the given organization and marketplace application,
     * applying pagination and sorting configurations
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param limitConfig      the pagination configuration specifying result limits
     * @param sortConfig       the sorting configuration specifying order of results
     * @return A collection of {@link CrmTeleCMIUser}
     */
    Collection<CrmTeleCMIUser> getUsers(final long orgId, final long marketplaceAppId,
                                        final LimitConfig limitConfig, final SortConfig sortConfig);

    /**
     * <p>
     * Fetches the total number of TeleCMI users for the given organization and marketplace application
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @return The total number of users
     */
    long getUserTotalRecords(final long orgId, final long marketplaceAppId);

    /**
     * <p>
     * Checks whether a user exists for the given organization and marketplace application
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param marketplaceAppId Represents the marketplace application ID
     * @param userId           Represents the user id
     * @return true if the user exists, false otherwise
     */
    boolean isUserExist(final long orgId, final long marketplaceAppId, final long userId);

    /**
     * <p>
     * Finds a CRM TeleCMI user based on the provided TeleCMI ID.
     * </p>
     *
     * @param teleCMIUserId Represents the unique TeleCMI ID of the user
     * @return An {@link Optional} containing the {@link CrmTeleCMIUser} if found; otherwise, an empty Optional
     */
    Optional<CrmTeleCMIUser> findUser(final String teleCMIUserId);

    /**
     * <p>
     * Checks whether an application with the given appId exists
     * </p>
     *
     * @param appId            Represents an app id
     * @param marketplaceAppId Represents the marketplace application ID.
     * @return true if a configuration entry exists with the specified app ID and secret; false otherwise
     */
    boolean isAppIdExists(final Long appId, final long marketplaceAppId);

    /**
     * Fetches a collection of {@link CrmTeleCMIUser} user for the given organization, marketplace app, and user IDs.
     *
     * @param orgId   Represents an organization ID
     * @param userIds Which contains the collection of user ids
     * @return The collection of {@link CrmTeleCMIUser}
     */
    Collection<CrmTeleCMIUser> getUsers(final long orgId, final Collection<Long> userIds);

    /**
     * <p>
     * Gets the user ids for the given org id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param marketplaceAppId Represents the marketplace app id
     * @return The collection of user ids
     */
    Collection<Long> getUserIds(final long orgId, final long marketplaceAppId);
}

package com.tlc.iam.client;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.client.internal.action.UserServiceImpl;
import com.tlc.iam.client.models.PasswordSetRequest;
import com.tlc.iam.common.models.User;
import com.tlc.web.client.event.EventCallback;

import java.util.Collection;
import java.util.Map;

/**
 * The {@link UserService} provides the service for the user.
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface UserService {

    /**
     * Fetches the {@link UserService} instance
     *
     * @return the instance of {@link UserService}
     */
    static UserService getInstance() {
        return UserServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userIds  Represents the collection of user Ids to retrieve
     * @param callback The callback to handle the fetched user data.
     */
    void fetchUsers(final long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Fetches the collection of users by the given email addresses
     * </p>
     *
     * @param emailAddresses Represents the email addresses
     * @param callback       The callback to handle the fetched users data
     */
    void fetchUsersByEmails(final Collection<String> emailAddresses, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param isPartial    A flag is represents the email is to be fetched or not
     * @param searchFilter Represents the {@link SearchFilter} details
     * @param callback     The callback to handle the fetched user data.
     */
    void fetchUsers(final boolean isPartial, final long orgId, final SearchFilter searchFilter, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isPartial     A flag is represents the email is to be fetched or not
     * @param searchFilters Which contains the collection of {@link SearchFilter} details
     * @param callback      The callback to handle the fetched user data.
     */
    void fetchUsers(final boolean isPartial, final long orgId, final Collection<SearchFilter> searchFilters, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Fetches user from the specified organization based on the given user Id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param callback The callback to handle the fetched user data
     */
    void fetchUser(final Long orgId, final Long userId, final EventCallback<User> callback);

    /**
     * <p>
     * Saves or updates a user, handling both creation and update events, if isCreateEvent is true,
     * it indicates that the user is being created; otherwise, the user is updated
     * </p>
     *
     * @param orgId              Represents the organization id
     * @param isCreateEvent      A flag indicating whether the operation is triggered by a create event
     * @param user               An entity that holds the {@link User} details
     * @param userUpsertCallBack The callback to be executed after the upsert operation
     */
    void upsert(final long orgId, final boolean isCreateEvent, final User user, final EventCallback<JsonObject> userUpsertCallBack);

    /**
     * <p>
     * Fetches user from the specified organization based on the given user Ids and organization id.
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param userIds   Represents the collection of user Ids to retrieve
     * @param callbacks The callback to handle the fetched users data
     */
    void fetchUser(final Long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callbacks);

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs & emails.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userIds  Represents the collection of user Ids to retrieve
     * @param emails   Represents the collection of user's email address
     * @param callback The callback to handle the fetched user data.
     */
    void fetchUsers(final long orgId, final Collection<Long> userIds, final Collection<String> emails, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Partial bulk update of user information for a given organization
     * </p>
     *
     * @param iamOrgId                  Represents an organization id
     * @param users                     Which contains collection of {@link User}
     * @param userPartialUpdateCallBack The callback function for handling events related to user partial update actions
     */
    void partialBulkUpdate(final Long iamOrgId, final Collection<User> users, final EventCallback<JsonObject> userPartialUpdateCallBack);

    /**
     * <p>
     * Updates the user state de activated state into active state
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param iamUserIds    Which contains the collection of user ids
     * @param state         Represents the user state
     * @param eventCallback The callback to be user state update operation
     */
    void updateState(final Long orgId, final Collection<Long> iamUserIds, final User.State state, final EventCallback<JsonObject> eventCallback);

    /**
     * <p>
     * Fetches users from the specified organization based on the start and limit condition.
     * </p>
     *
     * @param orgId              Represents an organization id
     * @param searchFilter       Represents the {@link SearchFilter} details
     * @param usersFetchCallback The callback to handle the fetched user data.
     */
    void fetchUsers(final Long orgId, final SearchFilter searchFilter, final EventCallback<Collection<User>> usersFetchCallback);

    /**
     * <p>
     * Partially update of user information for a given organization
     * </p>
     *
     * @param iamOrgId                  Represents an organization id
     * @param userJson                  Which contains {@link User} details
     * @param userPartialUpdateCallBack The callback function for handling events related to user partial update actions
     */
    void partialUpdate(final Long iamOrgId, final JsonObject userJson, final EventCallback<JsonObject> userPartialUpdateCallBack);

    /**
     * <p>
     * Resets the password for the given user
     * </p>
     *
     * @param user          Represents the {@link User} details
     * @param eventCallback The callback to handle the set the user password.
     */
    void resetPassword(final User user, final EventCallback<JsonObject> eventCallback);

    /**
     * Retrieves users associated with the provided names within a specified organization
     *
     * @param orgId              The ID of the organization
     * @param userNames          A collection of usernames
     * @param usersFetchCallback The callback function to manage the retrieved user data
     */
    void fetchUsersByNames(final long orgId, final Collection<String> userNames, final EventCallback<Collection<User>> usersFetchCallback);

    /**
     * <p>
     * Updates the account details of the organization.
     * </p>
     *
     * @param orgId                        Represents the organization id
     * @param user                         Represents the {@link User}.
     * @param accountDetailsUpdateCallback Represents the callback function for handling account details update event.
     */
    void updateAccountDetails(final long orgId, final User user, final EventCallback<JsonObject> accountDetailsUpdateCallback);

    /**
     * <p>
     * Fetches users along with their associated organization
     * </p>
     * @param orgId    Represents the org id
     * @param userIds  Which contains the collection of user ids
     * @param callback An event callback to handle the response containing the collection of users.
     */
    void fetchUsersWithOrg(final Long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callback);

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isPartial     A flag is representing the email is to be fetched or not
     * @param searchFilters Which contains the collection of {@link SearchFilter} details
     * @param callback      The callback to handle the fetched user data
     */
    void fetchUserPage(final boolean isPartial, final long orgId, final Collection<SearchFilter> searchFilters,
                       final EventCallback<Page<User>> callback);

    /**
     * <p>
     * Fetches the user by given email.
     * </p>
     *
     * @param email             Represents the email address
     * @param userEventCallback Represents the even callback for user
     */
    void fetchUserByEmail(final String email, final EventCallback<User> userEventCallback);

    /**
     * Fetches the users by the given phone numbers.
     *
     * @param phoneNumbers      the phone number to look up
     * @param userEventCallback the callback to handle the fetched user data
     */
    void fetchUsersByPhones(final Collection<String> phoneNumbers, final EventCallback<Collection<User>> userEventCallback);

    /**
     * <p>
     * Sets the password for the given user
     * </p>
     *
     * @param passwordSetRequest Holds the data for resetting password
     * @param eventCallback      Represents the callback to handle setting the user password
     */
    void setPassword(final PasswordSetRequest passwordSetRequest, final EventCallback<JsonObject> eventCallback);

    /**
     * <p>
     *  Searches for users within the specified organization based on the provided search filters
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isPartial     Whether to retrieve a partial user list or a full list.
     * @param searchFilters A collection of search filters to apply to the user
     * @param callback      The callback function to handle the retrieved user page
     */
    void searchUsers(final long orgId, final boolean isPartial, final Collection<SearchFilter> searchFilters,
                     final EventCallback<Page<User>> callback);

    /**
     * <p>
     * Initiates an asynchronous check to verify whether all specified users of an organization are in an active state
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userIds  Which contains collection of user ids
     * @param callback the callback to be invoked once the asynchronous operation completes
     */
    void checkAllUsersActive(final long orgId, final Collection<Long> userIds, final EventCallback<Boolean> callback);

    /**
     * <p>
     * Initiates an asynchronous check to verify whether any of the specified users of an organization are in a deactivated state
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userIds  Which contains collection of user ids
     * @param callback the callback to be invoked once the asynchronous operation completes
     */
    void hasAnyDeactivatedUser(final long orgId, final Collection<Long> userIds, final EventCallback<Boolean> callback);

    /**
     * <p>
     * Fetches users from the specified organization based on the user state
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param searchFilters Which contains the collection of {@link SearchFilter} details
     * @param callback      The callback to handle the fetched user data
     */
    void fetchUserPageByState(final long orgId, final Collection<SearchFilter> searchFilters, final EventCallback<Page<User>> callback);

    /**
     * Fetches the active user details for the supplied IAM user IDs and returns them keyed by user ID.
     *
     * @param orgId    Represents the organization id for the lookup context
     * @param userIds  Represents the IAM user IDs to fetch the active user data for
     * @param callback The callback that receives the resolved map of user IDs to {@link User} thumbnail
     */
    void fetchActiveUsersThumbnail(final long orgId, final Collection<Long> userIds, final EventCallback<Map<Long, DisplayPicture>> callback);
}
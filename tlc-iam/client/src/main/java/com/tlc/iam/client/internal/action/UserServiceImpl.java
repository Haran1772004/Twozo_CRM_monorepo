package com.tlc.iam.client.internal.action;

import com.tlc.attachment.model.profilePicture.DisplayPicture;
import com.tlc.attachment.model.profilePicture.ProfilePicture;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.page.Page;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.iam.client.models.PasswordSetRequest;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.OrganizationFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.ProfilePictureConverter;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * The {@link UserServiceImpl} implements the {@link UserService}.
 * It provides the implementation for various operations related to the users.
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class UserServiceImpl implements UserService {

    private final String userNotFoundI18NKey = "i18n.iam.accounts.user.not.found";
    private final UserConverter userConverter;
    private final ProfilePictureConverter profilePictureConverter;

    public UserServiceImpl() {
        userConverter = UserConverter.getInstance();
        profilePictureConverter = ProfilePictureConverter.getInstance();
    }

    private static class Instance {
        private static final UserServiceImpl INSTANCE = new UserServiceImpl();
    }

    public static UserServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchUsers(final long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callback) {
        final JsonObject data = Json.object();

        data.put(UserFields.IDS, userIds);
        data.put(UserFields.ORG_ID, orgId);
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, data.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddresses Represents the email addresses
     * @param callback       The callback to handle the fetched user data
     */
    @Override
    public void fetchUsersByEmails(final Collection<String> emailAddresses, final EventCallback<Collection<User>> callback) {
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, Json.object().put(UserFields.EMAILS, emailAddresses).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new HashSet<>();

                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final JsonObject jsonObject = jsonArray.getJsonObject(index);
                        final User user = userConverter.convert(jsonObject.getLong(UserFields.ORG_ID), jsonObject);

                        users.add(user);
                    }
                }
                callback.onSuccess(users);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * <p>
     * Fetches users from the specified organization based on the given user IDs.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param isPartial    A flag is representing the email is to be fetched or not
     * @param searchFilter Represents the {@link SearchFilter} details
     * @param callback     The callback to handle the fetched user data.
     */
    @Override
    public void fetchUsers(final boolean isPartial, final long orgId, final SearchFilter searchFilter, final EventCallback<Collection<User>> callback) {
        final JsonObject jsonObject = Json.object();
        final String type = searchFilter.getType();
        final String stateKey = IamFields.STATE;
        final Collection<Long> userIds = searchFilter.getUserIds();

        if (Objects.nonNull(type)) {
            final JsonObject filterJson = Json.object();

            filterJson.put(IamFields.TYPE, type);
            filterJson.put(IamFields.VALUE, searchFilter.getValue());
            jsonObject.put(Objects.equals(stateKey, type) ? stateKey : IamFields.CRITERIA, filterJson);
        }
        jsonObject.put(IamFields.START, searchFilter.getStart());
        jsonObject.put(IamFields.LIMIT, searchFilter.getLimit());
        jsonObject.put(IamFields.IS_PARTIAL, isPartial);
        jsonObject.put(IamFields.ORG_ID, orgId);
        jsonObject.put(IamFields.EXCLUDE_INVITED_USER, searchFilter.isExcludeInvitedUsers());
        jsonObject.put(IamFields.INCLUDE_DEACTIVATED_USER, searchFilter.isIncludeDeactivatedUsers());

        if (Objects.nonNull(userIds) && !userIds.isEmpty()) {
            jsonObject.put(UserFields.IDS, userIds);
        }
        final Event<byte[]> usersFetchEvent = new Event<>(IamEvents.FETCH_USERS, jsonObject.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new HashSet<>();

                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                }
                callback.onSuccess(users);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersFetchEvent, eventCallback);
    }

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
    @Override
    public void fetchUsers(final boolean isPartial, final long orgId, final Collection<SearchFilter> searchFilters, final EventCallback<Collection<User>> callback) {
        final JsonObject jsonObject = Json.object();

        loadSearchFilterJson(orgId, isPartial, jsonObject, searchFilters);
        final Event<byte[]> usersFetchEvent = new Event<>(IamEvents.FETCH_USERS, jsonObject.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new HashSet<>();

                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                }
                callback.onSuccess(users);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersFetchEvent, eventCallback);
    }

    /**
     * <p>
     * load the search filter details into the json.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isPartial     A flag is representing the email is to be fetched or not
     * @param searchFilters Which contains the collection of {@link SearchFilter} details
     */
    private void loadSearchFilterJson(final Long orgId, final boolean isPartial, final JsonObject jsonObject,
                                      final Collection<SearchFilter> searchFilters) {
        final String stateKey = IamFields.STATE;
        final String criteriaKey = IamFields.CRITERIA;
        final String typeKey = IamFields.TYPE;
        final String valueKey = IamFields.VALUE;
        final String startKey = IamFields.START;
        final String limitKey = IamFields.LIMIT;
        final String excludeInvitedUserKey = IamFields.EXCLUDE_INVITED_USER;
        final String includeDeactivatedUserKey = IamFields.INCLUDE_DEACTIVATED_USER;

        for (final SearchFilter searchFilter : searchFilters) {
            final String type = searchFilter.getType();
            final Object value = searchFilter.getValue();
            final Integer start = searchFilter.getStart();
            final Integer limit = searchFilter.getLimit();
            final Boolean excludeInvitedUsers = searchFilter.isExcludeInvitedUsers();
            final Boolean includeDeactivatedUsers = searchFilter.isIncludeDeactivatedUsers();
            final Boolean isExcludeWithoutPhone = searchFilter.excludeUsersWithoutPhone();
            final Collection<Long> userIds = searchFilter.getUserIds();
            final Collection<Long> excludeUserIds = searchFilter.getExcludeUserIds();

            if (Objects.nonNull(type)) {
                final JsonObject filterJson = Json.object();

                filterJson.put(typeKey, type);
                filterJson.put(valueKey, value);
                jsonObject.put(Objects.equals(stateKey, type) ? stateKey : criteriaKey, filterJson);
            }

            if (Objects.nonNull(start)) {
                jsonObject.put(startKey, start);
            }

            if (Objects.nonNull(limit)) {
                jsonObject.put(limitKey, limit);
            }

            if (Objects.nonNull(includeDeactivatedUsers)) {
                jsonObject.put(includeDeactivatedUserKey, includeDeactivatedUsers);
            }

            if (Objects.nonNull(excludeInvitedUsers)) {
                jsonObject.put(excludeInvitedUserKey, excludeInvitedUsers);
            }

            if (Objects.nonNull(userIds)) {
                jsonObject.put(IamFields.USER_IDS, userIds);
            }

            if (Objects.nonNull(excludeUserIds)) {
                jsonObject.put(IamFields.EXCLUDE_USER_IDS, excludeUserIds);
            }

            if (Objects.nonNull(isExcludeWithoutPhone)) {
                jsonObject.put(IamFields.EXCLUDE_USERS_WITHOUT_PHONE, searchFilter.excludeUsersWithoutPhone());
            }
        }
        jsonObject.put(IamFields.IS_PARTIAL, isPartial);
        jsonObject.put(IamFields.ORG_ID, orgId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param callback The callback to handle the fetched user data.
     */
    @Override
    public void fetchUser(final Long orgId, final Long userId, final EventCallback<User> callback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(UserFields.ORG_ID, orgId);
        jsonObject.put(UserFields.ID, userId);
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USER, jsonObject.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    callback.onSuccess(userConverter.convert(orgId, Json.object(bytes)));
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents the organization id
     * @param isCreateEvent      A flag indicating whether the operation is triggered by a create event
     * @param user               An entity that holds the {@link User} details
     * @param userUpsertCallBack The callback to be executed after the upsert operation
     */
    @Override
    public void upsert(final long orgId, final boolean isCreateEvent, final User user, final EventCallback<JsonObject> userUpsertCallBack) {
        final JsonObject userJson = userConverter.convert(user, true);

        userJson.put(IamFields.IS_CREATE_EVENT, isCreateEvent);

        final Event<byte[]> createHandler = new Event<>(IamEvents.USER_UPSERT, userJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                userUpsertCallBack.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                userUpsertCallBack.onFailure(ErrorCode.get(IamErrorCodes.UPSERT_ACTION_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(createHandler, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userIds  Represents the collection of user id
     * @param callback The callback to handle the fetched user data
     */
    @Override
    public void fetchUser(final Long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callback) {
        final JsonObject data = Json.object();

        data.put(UserFields.IDS, userIds);
        data.put(UserFields.ORG_ID, orgId);
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, data.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

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
    @Override
    public void fetchUsers(final long orgId, Collection<Long> userIds, final Collection<String> emails,
                           final EventCallback<Collection<User>> callback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(UserFields.IDS, userIds);
        jsonObject.put(UserFields.EMAILS, emails);
        jsonObject.put(UserFields.ORG_ID, orgId);
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, jsonObject.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId                     Represents an organization id
     * @param users                     Which contains collection of {@link User}
     * @param userPartialUpdateCallBack The callback function for handling events related to user partial update actions
     */
    @Override
    public void partialBulkUpdate(final Long orgId, final Collection<User> users, final EventCallback<JsonObject> userPartialUpdateCallBack) {
        final JsonObject userJson = Json.object();
        final JsonArray jsonArray = Json.array();

        for (final User user : users) {
            jsonArray.put(userConverter.convert(user, false));
        }
        userJson.put(UserFields.USER, jsonArray);

        final Event<byte[]> usersPartialUpdateHandler = new Event<>(IamEvents.USER_PARTIAL_BULK_UPDATE, userJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                userPartialUpdateCallBack.onSuccess(Json.object(data));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                userPartialUpdateCallBack.onFailure(ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersPartialUpdateHandler, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param userIds       Which contains the collection of user ids
     * @param state         Represents the user state
     * @param eventCallback The callback to be user state update operation
     */
    @Override
    public void updateState(final Long orgId, final Collection<Long> userIds, final User.State state, final EventCallback<JsonObject> eventCallback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(IamFields.ORG_ID, orgId);
        jsonObject.put(IamFields.USER_IDS, userIds);
        jsonObject.put(IamFields.STATE, state);
        final Event<byte[]> userStateUpdateHandler = new Event<>(IamEvents.USER_STATE_UPDATE, jsonObject.getBytes());

        final EventCallback<byte[]> updateEventCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                eventCallback.onSuccess(Json.object(data));
            }

            @Override
            public void onFailure(Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userStateUpdateHandler, updateEventCallBack);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              Represents an organization id
     * @param searchFilter       Represents the {@link SearchFilter} details
     * @param usersFetchCallback The callback to handle the fetched user data.
     */
    @Override
    public void fetchUsers(final Long orgId, final SearchFilter searchFilter, final EventCallback<Collection<User>> usersFetchCallback) {
        final JsonObject jsonObject = Json.object();
        final String type = searchFilter.getType();
        final Object value = searchFilter.getValue();

        if (Objects.nonNull(type)) {
            final JsonObject criteriaJson = Json.object();

            criteriaJson.put(IamFields.TYPE, type);
            criteriaJson.put(IamFields.VALUE, value);
            jsonObject.put(IamFields.CRITERIA, criteriaJson);
        }

        jsonObject.put(IamFields.START, searchFilter.getStart());
        jsonObject.put(IamFields.LIMIT, searchFilter.getLimit());
        jsonObject.put(IamFields.ORG_ID, orgId);
        jsonObject.put(IamFields.IS_PARTIAL, false);

        final Event<byte[]> usersFetchEvent = new Event<>(IamEvents.FETCH_USERS, jsonObject.getBytes());

        final EventCallback<byte[]> usersEventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new HashSet<>();

                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }
                }
                usersFetchCallback.onSuccess(users);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                usersFetchCallback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersFetchEvent, usersEventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId                  Represents an organization id
     * @param userJson                  Which contains {@link User} details
     * @param userPartialUpdateCallBack The callback function for handling events related to user partial update actions
     */
    @Override
    public void partialUpdate(final Long iamOrgId, final JsonObject userJson, final EventCallback<JsonObject> userPartialUpdateCallBack) {
        final Event<byte[]> userPartialUpdateHandler = new Event<>(IamEvents.USER_PARTIAL_UPDATE, userJson.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                userPartialUpdateCallBack.onSuccess(Json.object(data));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                userPartialUpdateCallBack.onFailure(ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userPartialUpdateHandler, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param user          Represents the {@link User} details
     * @param eventCallback The callback to handle the set the user password.
     */
    @Override
    public void resetPassword(final User user, final EventCallback<JsonObject> eventCallback) {
        final JsonObject jsonObject = userConverter.convert(user, true);
        final Event<byte[]> passwordResetEvent = new Event<>(IamEvents.RESET_PASSWORD, jsonObject.getBytes());
        final EventCallback<byte[]> passwordResetCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                eventCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.PASSWORD_RESET_ACTION_FAILED, "i18n.iam.accounts.password.reset.failed"));
            }
        };

        EventNotifier.get().send(passwordResetEvent, passwordResetCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId              The ID of the organization
     * @param userNames          A collection of usernames
     * @param usersFetchCallback The callback function to manage the retrieved user data
     */
    @Override
    public void fetchUsersByNames(final long orgId, final Collection<String> userNames, final EventCallback<Collection<User>> usersFetchCallback) {
        final JsonObject requestJson = Json.object();
        requestJson.put(UserFields.ORG_ID, orgId);
        requestJson.put(UserFields.NAMES, userNames);

        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, requestJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {

                if (Objects.isNull(bytes)) {
                    usersFetchCallback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                } else {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final JsonObject jsonObject = jsonArray.getJsonObject(index);
                        final User user = userConverter.convert(jsonObject.getLong(UserFields.ORG_ID), jsonObject);

                        users.add(user);
                    }
                    usersFetchCallback.onSuccess(users);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                usersFetchCallback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId                     Represents the organization id.
     * @param user                         Represents the {@link User}.
     * @param accountDetailsUpdateCallback Represents the callback function for handling account details update event.
     */
    @Override
    public void updateAccountDetails(final long iamOrgId, final User user, final EventCallback<JsonObject> accountDetailsUpdateCallback) {
        final JsonObject userJson = Json.object();

        userJson.put(UserFields.USER, userConverter.convert(user, false));
        final Event<byte[]> accountDetailsUpdateEvent = new Event<>(IamEvents.ACCOUNT_DETAILS_UPDATE, userJson.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            /**
             * {@inheritDoc}
             *
             * @param data Represents the response data from the consumer.
             */
            @Override
            public void onSuccess(final byte[] data) {
                accountDetailsUpdateCallback.onSuccess(Json.object(data));
            }

            /**
             * {@inheritDoc}
             *
             * @param throwable Represents the cause of the failure.
             */
            @Override
            public void onFailure(final Throwable throwable) {
                accountDetailsUpdateCallback.onFailure(ErrorCode.get(IamErrorCodes.UPDATE_ACTION_FAILED, throwable.getMessage()));
            }
        };

        EventNotifier.get().send(accountDetailsUpdateEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the org id
     * @param userIds  Which contains the collection of user ids
     * @param callback An event callback to handle the response containing the collection of users.
     */
    @Override
    public void fetchUsersWithOrg(final Long orgId, final Collection<Long> userIds, final EventCallback<Collection<User>> callback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(UserFields.IDS, userIds);
        jsonObject.put(UserFields.IS_ORG_REQUIRED, true);
        jsonObject.put(UserFields.ORG_ID, orgId);
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, jsonObject.getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final JsonObject userJson = jsonArray.getJsonObject(index);
                        final User user = userConverter.convert(userJson.getLong(UserFields.ORG_ID), userJson);

                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </P>
     *
     * @param email    Represents the email address
     * @param callback Represents the even callback for user
     */
    @Override
    public void fetchUserByEmail(final String email, final EventCallback<User> callback) {
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USER, Json.object().put(UserFields.EMAIL, email).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            /**
             * <p>
             *    {@inheritDoc}
             * </p>
             *
             * @param bytes Response from the consumer
             */
            @Override
            public void onSuccess(final byte[] bytes) {

                if (Objects.isNull(bytes)) {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                } else {
                    final JsonObject userJson = Json.object(bytes);

                    callback.onSuccess(userConverter.convert(userJson.getLong(IamFields.ORG_ID), userJson));
                }
            }

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fetchUsersByPhones(final Collection<String> phoneNumbers, final EventCallback<Collection<User>> callback) {
        final Event<byte[]> userFetchEvent = new Event<>(IamEvents.FETCH_USERS, Json.object().put(UserFields.MOBILE, phoneNumbers).getBytes());
        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final String jsonString = new String(bytes);
                    final JsonArray jsonArray = Json.array(jsonString);
                    final Collection<User> users = new HashSet<>();

                    for (int index = 0; index < jsonArray.size(); index++) {
                        final JsonObject userJson = jsonArray.getJsonObject(index);
                        final User user = userConverter.convert(userJson.getLong(IamFields.ORG_ID), userJson);

                        users.add(user);
                    }
                    callback.onSuccess(users);
                } else {
                    callback.onFailure(ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, userNotFoundI18NKey));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(userFetchEvent, eventCallback);
    }

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
    @Override
    public void fetchUserPage(final boolean isPartial, final long orgId,
                              final Collection<SearchFilter> searchFilters, final EventCallback<Page<User>> callback) {
        fetchUserPage(orgId, isPartial, IamEvents.FETCH_USER_PAGE, searchFilters, callback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents an organization id
     * @param isPartial     Whether to retrieve a partial user list or a full list
     * @param searchFilters A collection of search filters to apply to the user
     * @param callback      The callback function to handle the retrieved user page
     */
    @Override
    public void searchUsers(final long orgId, final boolean isPartial,
                            final Collection<SearchFilter> searchFilters, final EventCallback<Page<User>> callback) {
        fetchUserPage(orgId, isPartial, IamEvents.SEARCH_USER_PAGE, searchFilters, callback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userIds  Which contains collection of user ids
     * @param callback the callback to be invoked once the asynchronous operation completes
     */
    @Override
    public void checkAllUsersActive(final long orgId, final Collection<Long> userIds, final EventCallback<Boolean> callback) {
        final JsonObject data = Json.object();

        data.put(IamFields.ORG_ID, orgId);
        data.put(IamFields.USER_IDS, userIds);

        final EventCallback<Boolean> eventCallback = new EventCallback<>() {
            @Override
            public void onSuccess(final Boolean hasAllUsersActive) {
                callback.onSuccess(hasAllUsersActive);
            }

            @Override
            public void onFailure(final Throwable cause) {
                callback.onFailure(cause);
            }
        };
        EventNotifier.get().send(new Event<>(IamEvents.ACTIVE_USERS_CHECK, data.getBytes()), eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents an organization id
     * @param userIds  Which contains collection of user ids
     * @param callback the callback to be invoked once the asynchronous operation completes
     */
    @Override
    public void hasAnyDeactivatedUser(final long orgId, final Collection<Long> userIds, final EventCallback<Boolean> callback) {
        final JsonObject data = Json.object();

        data.put(IamFields.ORG_ID, orgId);
        data.put(IamFields.USER_IDS, userIds);

        final EventCallback<Boolean> eventCallback = new EventCallback<>() {
            @Override
            public void onSuccess(final Boolean hasDeactivatedUser) {
                callback.onSuccess(hasDeactivatedUser);
            }

            @Override
            public void onFailure(final Throwable cause) {
                callback.onFailure(cause);
            }
        };
        EventNotifier.get().send(new Event<>(IamEvents.DEACTIVATED_USERS_CHECK, data.getBytes()), eventCallback);
    }

    /**
     * <p>
     * Fetches the user page for the given organization id
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param isPartial     Whether to retrieve a partial user list or a full list
     * @param eventName     Represents an event name
     * @param searchFilters A collection of search filters to apply to the user
     * @param callback      The callback function to handle the retrieved user page
     */
    private void fetchUserPage(final long orgId, final boolean isPartial, final String eventName,
                               final Collection<SearchFilter> searchFilters, final EventCallback<Page<User>> callback) {
        final JsonObject jsonObject = Json.object();

        loadSearchFilterJson(orgId, isPartial, jsonObject, searchFilters);
        final Event<byte[]> usersFetchEvent = new Event<>(eventName, jsonObject.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new HashSet<>();
                final Page<User> userPage = new Page<>();

                if (Objects.nonNull(bytes)) {
                    final JsonObject userJson = Json.object(bytes);
                    final int totalRecords = userJson.getInt(UserFields.TOTAL_RECORDS);
                    final JsonArray jsonArray = userJson.getJsonArray(UserFields.USERS);
                    final int size = jsonArray.size();

                    for (int index = 0; index < size; index++) {
                        final User user = userConverter.convert(orgId, jsonArray.getJsonObject(index));

                        users.add(user);
                    }

                    userPage.setData(users);
                    userPage.setNumber(totalRecords);
                }
                callback.onSuccess(userPage);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersFetchEvent, eventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param passwordSetRequest Holds the data for setting password
     * @param eventCallback      Represents the callback to handle setting the user password
     */
    @Override
    public void setPassword(final PasswordSetRequest passwordSetRequest, final EventCallback<JsonObject> eventCallback) {
        final JsonObject jsonObject = userConverter.convert(passwordSetRequest.user(), true);

        jsonObject.put(IamFields.APPLICATION, passwordSetRequest.applicationName());
        jsonObject.put(OrganizationFields.USER_AGENT, passwordSetRequest.userAgent().toJson());
        final Event<byte[]> passwordResetEvent = new Event<>(IamEvents.SET_PASSWORD, jsonObject.getBytes());
        final EventCallback<byte[]> passwordResetCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                eventCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.PASSWORD_RESET_ACTION_FAILED, "i18n.iam.accounts.password.reset.failed"));
            }
        };

        EventNotifier.get().send(passwordResetEvent, passwordResetCallback);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param searchFilters Which contains the collection of {@link SearchFilter} details
     * @param callback      The callback to handle the fetched user data
     */
    @Override
    public void fetchUserPageByState(final long orgId, final Collection<SearchFilter> searchFilters, final EventCallback<Page<User>> callback) {
        final JsonObject jsonObject = Json.object();

        loadSearchFilterJson(orgId, false, jsonObject, searchFilters);
        final Event<byte[]> usersFetchEvent = new Event<>(IamEvents.FETCH_USER_PAGE_BY_STATE, jsonObject.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                final Collection<User> users = new LinkedHashSet<>();
                final Page<User> userPage = new Page<>();

                if (Objects.nonNull(bytes)) {
                    final JsonObject userJson = Json.object(bytes);
                    final int totalRecords = userJson.getInt(UserFields.TOTAL_RECORDS);
                    final JsonArray jsonArray = userJson.getJsonArray(UserFields.USERS);
                    final int size = jsonArray.size();

                    for (int index = 0; index < size; index++) {
                        users.add(userConverter.convert(orgId, jsonArray.getJsonObject(index)));
                    }

                    userPage.setData(users);
                    userPage.setNumber(totalRecords);
                }
                callback.onSuccess(userPage);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };
        EventNotifier.get().send(usersFetchEvent, eventCallback);
    }

    /**
     * Fetches the active user details for the supplied IAM user IDs and returns them keyed by user ID.
     *
     * <p>This method packages the organization ID and the requested user ID collection into an IAM event,
     * dispatches the thumbnail/user lookup request, and converts the response payload back to a map of user
     * records that can be used directly by the caller.</p>
     *
     * @param orgId    Represents the organization id for the lookup context
     * @param userIds  Represents the IAM user IDs to fetch the active user data for
     * @param callback The callback that receives the resolved map of user IDs to {@link User} details
     */
    public void fetchActiveUsersThumbnail(final long orgId, final Collection<Long> userIds, final EventCallback<Map<Long, DisplayPicture>> callback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(IamFields.ORG_ID, orgId);
        jsonObject.put(UserFields.USER_IDS, userIds);

        final Event<byte[]> usersFetchEvent = new Event<>(IamEvents.FETCH_ACTIVE_USERS_THUMBNAIL, jsonObject.getBytes());

        final EventCallback<byte[]> eventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                if (Objects.nonNull(bytes)) {
                    final JsonObject userJson = Json.object(bytes);
                    final JsonObject thumbnails = userJson.getJsonObject(IamFields.DATA);
                    final JsonArray userIds = userJson.getJsonArray(IamFields.USER_IDS);
                    final Map<Long, DisplayPicture> userMap = new HashMap<>();
                    final int userIdsSize = userIds.size();

                    for (int index = NumericalConstant.ZERO; index < userIdsSize; index++) {
                        final long userId = userIds.getLong(index);
                        final String userIdString = String.valueOf(userId);

                        if (thumbnails.containsKey(userIdString)) {
                            final DisplayPicture displayPicture = profilePictureConverter.constructDisplayPicture(
                                            thumbnails.getJsonObject(userIdString),
                                            ProfilePicture.Quality.THUMBNAIL
                                    );

                            userMap.put(userId, displayPicture);
                        } else {
                            userMap.put(userId, null);
                        }
                    }

                    callback.onSuccess(userMap);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                callback.onFailure(ErrorCode.get(IamErrorCodes.DATA_RETRIEVAL_FAILED, throwable.getMessage()));
            }
        };

        EventNotifier.get().send(usersFetchEvent, eventCallback);
    }
}
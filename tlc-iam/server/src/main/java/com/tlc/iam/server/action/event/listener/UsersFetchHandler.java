package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.filter.UserStateFilter;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.iam.server.internal.dropdown.SearchCriteria;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A listener that listens for events that is related to fetch the users
 * </p>
 *
 * @author PandiarajkumarG
 */
@EventListener(name = IamEvents.FETCH_USERS)
public class UsersFetchHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final String idsKey = UserFields.IDS;
        final String emailsKey = UserFields.EMAILS;
        final String valueKey =  IamFields.VALUE;
        final String namesKey = UserFields.NAMES;
        final String stateKey = IamFields.STATE;
        final String mobileKey = UserFields.MOBILE;
        final JsonObject jsonObject = Json.object(bytes);
        final boolean isOrdRequired = jsonObject.optBoolean(UserFields.IS_ORG_REQUIRED);
        final Collection<User> users = new HashSet<>();
        final UserManager userManager = UserManager.getInstance();
        final boolean isStateKeyExist = jsonObject.containsKey(stateKey);
        final Short stateId;
        boolean isStateFilterApplied = false;

        if (isStateKeyExist) {
            final UserStateFilter stateFilter = UserStateFilter.get(jsonObject.getJsonObject(stateKey).getString(valueKey));
            stateId = Objects.equals(UserStateFilter.ALL_USERS, stateFilter) ? null : stateFilter.id() ;
        } else {
            stateId = null;
        }

        if (jsonObject.containsKey(idsKey)) {
            final JsonArray jsonArray = jsonObject.getJsonArray(idsKey);
            final Collection<Long> userIds = new HashSet<>();

            for (int index = 0; index < jsonArray.size(); index++) {
                userIds.add(jsonArray.getLong(index));
            }

            if (jsonObject.containsKey(emailsKey)) {
                users.addAll(userManager.getUsers(userIds, getUserEmails(jsonObject)));
            } else {
                isStateFilterApplied = true;

                for (final Collection<Long> batchedUserIds : JavaUtil.splitCollection(userIds, NumericalConstant.FIFTY)) {
                    users.addAll(userManager.getUsers(jsonObject.getLong(UserFields.ORG_ID), batchedUserIds, isOrdRequired, stateId));
                }
            }
        } else if (jsonObject.containsKey(emailsKey)) {
            final Collection<String> userEmails = getUserEmails(jsonObject);

            for (final Collection<String> batchedUserEmails : JavaUtil.splitCollection(userEmails, NumericalConstant.FIFTY)) {
                users.addAll(userManager.getUsersByEmails(batchedUserEmails));
            }
        } else if (jsonObject.containsKey(namesKey)) {
            final JsonArray namesArray = jsonObject.getJsonArray(namesKey);
            final Collection<String> userNames = new HashSet<>();

            for (int index = 0; index < namesArray.size(); index++) {
                userNames.add(namesArray.getString(index));
            }

            for (final Collection<String> batchedUserNames : JavaUtil.splitCollection(userNames, NumericalConstant.FIFTY)) {
                users.addAll(userManager.getUsersByNames(jsonObject.getLong(UserFields.ORG_ID), batchedUserNames));
            }
        } else if (jsonObject.containsKey(mobileKey)) {
            final JsonArray mobilesArray = jsonObject.getJsonArray(mobileKey);
            final Collection<String> mobileNumbers = new HashSet<>();
            final long mobileArraySize = mobilesArray.size();

            for (int index = 0; index < mobileArraySize; index++) {
                mobileNumbers.add(mobilesArray.getString(index));
            }

            for (final Collection<String> batchedMobileNumbers : JavaUtil.splitCollection(mobileNumbers, NumericalConstant.FIFTY)) {
                if (jsonObject.containsKey(UserFields.ORG_ID)) {
                    users.addAll(userManager.getUsersByMobileNumbers(jsonObject.getLong(UserFields.ORG_ID), batchedMobileNumbers));
                } else {
                    users.addAll(userManager.getUsersByMobileNumbers(batchedMobileNumbers));
                }
            }
        }

        if (isStateKeyExist && !isStateFilterApplied) {
            setUsers(users, jsonObject, stateId);
        }

        if (users.isEmpty()) {
            return null;
        } else {
            final JsonArray jsonArray = Json.array();
            final UserConverter userConverter = UserConverter.getInstance();

            for (final User user : users) {
                jsonArray.put(userConverter.convert(user, jsonObject.optBoolean(UserFields.PASSWORD_REQUIRED, false)));
            }

            return jsonArray.getBytes();
        }
    }

    /**
     * <p>
     * Sets the collection of users based on the start and limit criteria
     * </p>
     *
     * @param users            Which contains the collection of users
     * @param jsonObject       Represents the {@link JsonObject} details
     */
    private void setUsers(final Collection<User> users, final JsonObject jsonObject, final Short stateId) {
        final UserManager userManager = UserManager.getInstance();
        final String criteriaKey = IamFields.CRITERIA;
        final SearchCriteria searchCriteria = new SearchCriteria();

        if (jsonObject.containsKey(criteriaKey)) {
            final String typeKey = IamFields.TYPE;
            final String valueKey = IamFields.VALUE;
            final JsonObject criteriaJson = jsonObject.getJsonObject(criteriaKey);

            if (criteriaJson.containsKey(typeKey)) {
                searchCriteria.setCriteriaType(criteriaJson.getString(typeKey));
            }

            if (criteriaJson.containsKey(valueKey)) {
                searchCriteria.setCriteriaValue(criteriaJson.getString(valueKey));
            }
        }
        users.addAll(userManager.getUsers(jsonObject.getLong(IamFields.ORG_ID), jsonObject.optBoolean(IamFields.IS_PARTIAL),
                jsonObject.getInt(IamFields.START), jsonObject.getInt(IamFields.LIMIT), searchCriteria, stateId,
                jsonObject.optBoolean(IamFields.INCLUDE_DEACTIVATED_USER), jsonObject.optBoolean(IamFields.EXCLUDE_INVITED_USER), Collections.emptySet(), false));
    }

    /**
     * <p>
     * Gets the {@link User} emails.
     * </p>
     *
     * @param jsonObject Holds user's details.
     * @return {@link Collection} of emails.
     */
    private Collection<String> getUserEmails(final JsonObject jsonObject) {
        final JsonArray emailsArray = jsonObject.getJsonArray(UserFields.EMAILS);
        final Collection<String> userEmails = new HashSet<>();

        for (int index = 0; index < emailsArray.size(); index++) {
            userEmails.add(emailsArray.getString(index));
        }

        return userEmails;
    }
}

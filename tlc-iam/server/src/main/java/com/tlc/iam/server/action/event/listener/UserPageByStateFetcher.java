package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.filter.UserStateFilter;

import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.iam.server.internal.dropdown.SearchCriteria;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * <p>
 * A listener that listens for events that is related to fetch the users with the count by state
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_USER_PAGE_BY_STATE)
public class UserPageByStateFetcher implements EventConsumer<byte[]> {

    private final UserManager userManager;
    private final UserConverter userConverter;

    public UserPageByStateFetcher() {
        userManager = UserManager.getInstance();
        userConverter = UserConverter.getInstance();
    }

    /**
     * Consumes the published content
     *
     * @param bytes Data to be consumed
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject requestJson = Json.object(bytes);
        final Long orgId = requestJson.getLong(IamFields.ORG_ID);
        final Short stateId = getStateId(requestJson);
        final SearchCriteria searchCriteria = getSearchCriteria(requestJson);
        final Collection<Long> userIds = getUserIds(orgId, requestJson, stateId);
        final Collection<User> users = new LinkedHashSet<>();
        final int start = requestJson.getInt(IamFields.START);
        final int limit = requestJson.getInt(IamFields.LIMIT);
        final boolean isSearchCriteriaIncluded = Objects.nonNull(searchCriteria.getValue());
        final boolean isFirstPageWithoutSearch = start == NumericalConstant.ONE && (!isSearchCriteriaIncluded);
        final long totalRecords = userManager.getTotalCount(orgId, stateId, searchCriteria);
        final JsonObject response = Json.object();
        final boolean isUserIdsEmpty = userIds.isEmpty();

        if (totalRecords > 0) {

            if (isFirstPageWithoutSearch) {
                if (isUserIdsEmpty) {
                    users.addAll(userManager.getUsers(orgId, start, limit, null, stateId, null));
                } else {
                    users.addAll(userManager.getUsers(orgId, userIds));
                    users.addAll(userManager.getUsers(orgId, start, limit - userIds.size(), null, stateId, userIds));
                }
            } else {
                if (isSearchCriteriaIncluded) {
                    users.addAll(userManager.getUsers(orgId, start, limit, searchCriteria, stateId, null));
                } else {
                    if (isUserIdsEmpty) {
                        users.addAll(userManager.getUsers(orgId, start, limit, null, stateId, null));
                    } else {
                        users.addAll(userManager.getUsers(orgId, start - userIds.size(), limit, null, stateId, userIds));
                    }
                }

            }
        }
        loadResponse(users, totalRecords, response);

        return response.getBytes();
    }

    /**
     * Extracts the state ID from the request JSON
     *
     * @param requestJson The request JSON object
     * @return The state ID or null if ALL_USERS filter is selected
     */
    private Short getStateId(final JsonObject requestJson) {
        if (requestJson.containsKey(IamFields.STATE)) {
            final JsonObject stateJson = requestJson.getJsonObject(IamFields.STATE);
            final UserStateFilter stateFilter = UserStateFilter.get(stateJson.getString(IamFields.VALUE));

            return Objects.equals(UserStateFilter.ALL_USERS, stateFilter) ? null : stateFilter.id();
        }

        return null;
    }

    /**
     * Extracts search criteria from the request JSON
     *
     * @param requestJson The request JSON object
     * @return The populated SearchCriteria object
     */
    private SearchCriteria getSearchCriteria(final JsonObject requestJson) {
        final SearchCriteria searchCriteria = new SearchCriteria();

        if (requestJson.containsKey(IamFields.CRITERIA)) {
            final JsonObject criteriaJson = requestJson.getJsonObject(IamFields.CRITERIA);

            if (criteriaJson.containsKey(IamFields.TYPE)) {
                searchCriteria.setCriteriaType(criteriaJson.getString(IamFields.TYPE));
            }

            if (criteriaJson.containsKey(IamFields.VALUE)) {
                searchCriteria.setCriteriaValue(criteriaJson.getString(IamFields.VALUE));
            }
        }

        return searchCriteria;
    }

     /**
      * Extracts additional user IDs from the request JSON
      *
      * @param requestJson The request JSON object
      * @param stateId     The state ID filter
      * @return Collection of additional user IDs to be included
      */
     private Collection<Long> getUserIds(final Long orgId, final JsonObject requestJson, final Short stateId) {
         if (requestJson.containsKey(IamFields.USER_IDS) && Objects.nonNull(stateId)) {
             final Collection<Long> userIds = userManager.groupUserIdsByState(orgId, (Collection<Long>) requestJson.get(IamFields.USER_IDS)).get(stateId);

             return Objects.nonNull(userIds) ? userIds : Collections.emptyList();
         }

         return Collections.emptyList();
     }

    /**
     * Loads the response JSON with users and total records count
     *
     * @param users        Collection of users to be included in response
     * @param totalRecords Total count of records
=     */
    private void loadResponse(final Collection<User> users, final long totalRecords, final JsonObject response) {
        final JsonArray userArray = Json.array();

        for (final User user : users) {
            userArray.put(userConverter.convert(user, false));
        }

        response.put(UserFields.TOTAL_RECORDS, totalRecords);
        response.put(UserFields.USERS, userArray);
    }
}
package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
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
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * A listener that listens for events that is related to fetch the search users page
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
@EventListener(name = IamEvents.SEARCH_USER_PAGE)
public class SearchUserPageFetcher implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param bytes Data to be consumed
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject jsonObject = Json.object(bytes);
        final Collection<User> users = new HashSet<>();
        final UserManager userManager = UserManager.getInstance();
        final SearchCriteria searchCriteria = new SearchCriteria();
        final Collection<Long> searchUserIds = new HashSet<>();
        long totalRecords = 0;
        boolean hasCriteria = false;

        if (jsonObject.containsKey(IamFields.STATE)) {
            final JsonObject stateJson = jsonObject.getJsonObject(IamFields.STATE);
            final UserStateFilter stateFilter = UserStateFilter.get(stateJson.getString(IamFields.VALUE));
            final Short stateId = Objects.equals(UserStateFilter.ALL_USERS, stateFilter) ? null : stateFilter.id();

            if (jsonObject.containsKey(IamFields.CRITERIA)) {
                final JsonObject criteriaJson = jsonObject.getJsonObject(IamFields.CRITERIA);

                if (criteriaJson.containsKey(IamFields.TYPE)) {
                    searchCriteria.setCriteriaType(criteriaJson.getString(IamFields.TYPE));
                }

                if (criteriaJson.containsKey(IamFields.VALUE)) {
                    final String criteriaValue = criteriaJson.getString(IamFields.VALUE);

                    searchCriteria.setCriteriaValue(criteriaValue);
                    hasCriteria = Objects.nonNull(criteriaValue);
                }
            }
            final Long orgId = jsonObject.getLong(IamFields.ORG_ID);
            final boolean isPartial = jsonObject.optBoolean(IamFields.IS_PARTIAL);
            final boolean includeDeactivatedUser = jsonObject.optBoolean(IamFields.INCLUDE_DEACTIVATED_USER);
            final boolean excludeInvitedUser = jsonObject.optBoolean(IamFields.EXCLUDE_INVITED_USER);
            totalRecords = userManager.getTotalCount(orgId, isPartial, searchCriteria, stateId, includeDeactivatedUser, excludeInvitedUser, Collections.emptySet(), false);

            if (totalRecords > 0) {

                if (jsonObject.containsKey(IamFields.USER_IDS)) {
                    final JsonArray searchUserIdJsonArray = jsonObject.getJsonArray(IamFields.USER_IDS);

                    searchUserIdJsonArray.forEach(userId -> searchUserIds.add((Long) userId));
                }
                final int configuredStart = jsonObject.getInt(IamFields.START);
                final int configuredLimit = jsonObject.getInt(IamFields.LIMIT);
                final boolean isSearchUser = configuredStart < 20;
                final int size = searchUserIds.size();
                final int start = isSearchUser ? configuredStart : configuredStart - size;
                final int limit = isSearchUser ? configuredLimit - size : configuredLimit;

                users.addAll(userManager.getUsers(jsonObject.getLong(IamFields.ORG_ID), jsonObject.optBoolean(IamFields.IS_PARTIAL),
                        start, limit, searchCriteria, stateId, jsonObject.optBoolean(IamFields.INCLUDE_DEACTIVATED_USER),
                        jsonObject.optBoolean(IamFields.EXCLUDE_INVITED_USER), isSearchUser, searchUserIds));
            }
        }
        final JsonObject result = Json.object();
        final JsonArray jsonArray = Json.array();

        if (!users.isEmpty()) {
            final UserConverter userConverter = UserConverter.getInstance();
            final boolean isPasswordRequired = jsonObject.optBoolean(UserFields.PASSWORD_REQUIRED, false);

            users.forEach(user -> jsonArray.put(userConverter.convert(user, isPasswordRequired)));
        }
        result.put(UserFields.TOTAL_RECORDS, totalRecords);
        result.put(UserFields.USERS, jsonArray);

        return result.getBytes();
    }
}
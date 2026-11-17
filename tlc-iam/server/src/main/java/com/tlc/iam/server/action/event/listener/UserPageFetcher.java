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
 * A listener that listens for events that is related to fetch the users with the count
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_USER_PAGE)
public class UserPageFetcher implements EventConsumer<byte[]> {

    /**
     * Consumes the published content
     *
     * @param bytes Data to be consumed
     */
    @Override
    public byte[] consume(final byte[] bytes) {
        final JsonObject jsonObject = Json.object(bytes);
        final Collection<User> users = new HashSet<>();
        final UserManager userManager = UserManager.getInstance();
        final SearchCriteria searchCriteria = new SearchCriteria();
        long totalRecords = 0;

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
                    searchCriteria.setCriteriaValue(criteriaJson.getString(IamFields.VALUE));
                }
            }
            final Collection<Long> excludeUserIds;

            if (jsonObject.containsKey(IamFields.EXCLUDE_USER_IDS)) {
                final JsonArray searchUserIdJsonArray = jsonObject.getJsonArray(IamFields.EXCLUDE_USER_IDS);
                excludeUserIds = new HashSet<>();

                searchUserIdJsonArray.forEach(userId -> excludeUserIds.add((Long) userId));
            } else {
                excludeUserIds = Collections.emptySet();
            }
            final Long orgId = jsonObject.getLong(IamFields.ORG_ID);
            final boolean isPartial = jsonObject.optBoolean(IamFields.IS_PARTIAL);
            final boolean includeDeactivatedUser = jsonObject.optBoolean(IamFields.INCLUDE_DEACTIVATED_USER);
            final boolean excludeInvitedUser = jsonObject.optBoolean(IamFields.EXCLUDE_INVITED_USER);
            final boolean isExcludeUserWithoutPhone = jsonObject.optBoolean(IamFields.EXCLUDE_USERS_WITHOUT_PHONE);
            totalRecords = userManager.getTotalCount(orgId, isPartial, searchCriteria, stateId, includeDeactivatedUser, excludeInvitedUser, excludeUserIds, isExcludeUserWithoutPhone);

            if (totalRecords > 0) {
                users.addAll(userManager.getUsers(jsonObject.getLong(IamFields.ORG_ID), jsonObject.optBoolean(IamFields.IS_PARTIAL),
                        jsonObject.getInt(IamFields.START), jsonObject.getInt(IamFields.LIMIT), searchCriteria, stateId,
                        jsonObject.optBoolean(IamFields.INCLUDE_DEACTIVATED_USER), jsonObject.optBoolean(IamFields.EXCLUDE_INVITED_USER),
                        excludeUserIds, isExcludeUserWithoutPhone));
            }
        }
        final JsonObject result = Json.object();
        final JsonArray jsonArray = Json.array();

        if (!users.isEmpty()) {
            final UserConverter userConverter = UserConverter.getInstance();

            for (final User user : users) {
                jsonArray.put(userConverter.convert(user, jsonObject.optBoolean(UserFields.PASSWORD_REQUIRED, false)));
            }
        }
        result.put(UserFields.TOTAL_RECORDS, totalRecords);
        result.put(UserFields.USERS, jsonArray);

        return result.getBytes();
    }
}

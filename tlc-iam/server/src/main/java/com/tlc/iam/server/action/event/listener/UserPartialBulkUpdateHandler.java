package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.common.models.validator.EntityValidator;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.validator.type.Group;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A listener that listens for user partial bulk update actions
 *
 * @author Kishore J
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_PARTIAL_BULK_UPDATE)
public class UserPartialBulkUpdateHandler implements EventConsumer<byte[]> {

    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject response = Json.object();
        final JsonObject userJson = Json.object(data);
        final UserManager userManager = UserManager.getInstance();
        final UserConverter userConverter = UserConverter.getInstance();
        final JsonArray userJsonArray = userJson.getJsonArray(UserFields.USER);
        final Collection<User> users = new ArrayList<>();
        final JsonArray jsonArray = Json.array();

        for (int index = 0; index < userJsonArray.size(); index++) {
            final JsonObject jsonObject = userJsonArray.getJsonObject(index);
            final Long userId = jsonObject.getLong(UserFields.ID);
            final User user = userManager.get(userId);

            userConverter.mapJsonDataToUser(user, jsonObject.getLong(UserFields.ORG_ID), jsonObject);
            users.add(user);
            jsonArray.put(userId);
        }
        final Collection<String> violations = EntityValidator.validate(users, Group.Update.class);

        if (violations.isEmpty()) {
            final List<Map<String, Object>> failedRecords = new ArrayList<>();
            final Collection<Long> updatedRecords = new HashSet<>();

            for (final User user : users) {
                final String mobile = user.getMobile();
                final Long userId = user.id();

                if (Objects.nonNull(mobile) && userManager.isMobileExists(user.orgId(), userId, mobile)) {
                    final Map<String, Object> entity = new HashMap<>();

                    entity.put(IamFields.ID, userId);
                    entity.put(IamFields.REASON, "i18n.iam.accounts.mobile.exists");
                    failedRecords.add(entity);
                } else {
                    userManager.update(user);
                    updatedRecords.add(userId);
                }
                response.put(IamFields.FAILED_RECORDS, failedRecords);
                response.put(IamFields.UPDATED_RECORDS, updatedRecords);
            }
        } else {
            response.put(IamFields.ERRORS, violations);
        }
        response.put(UserFields.IDS, jsonArray);

        return response.getBytes();
    }
}

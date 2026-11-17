package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
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
 * <p>
 * A listener that listens for user partial update actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_PARTIAL_UPDATE)
public class UserPartialUpdateHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param data Data to be consumed
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject response = Json.object();
        final JsonObject userJson = Json.object(data);
        final UserManager userManager = UserManager.getInstance();
        final UserConverter userConverter = UserConverter.getInstance();
        final Long userId = userJson.getLong(UserFields.ID);
        final User user = userManager.get(userId);

        userConverter.mapJsonDataToUser(user, userJson.getLong(UserFields.ORG_ID), userJson);
        final Collection<String> violations = EntityValidator.validate(List.of(user), Group.Update.class);

        if (violations.isEmpty()) {
            final List<Map<String, Object>> failedRecords = new ArrayList<>();
            final Collection<Long> updatedRecords = new HashSet<>();
            final String mobile = user.getMobile();

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
        } else {
            response.put(IamFields.ERRORS, violations);
        }
        response.put(UserFields.IDS, userJson);

        return response.getBytes();
    }
}

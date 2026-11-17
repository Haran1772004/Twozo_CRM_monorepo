package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.crypto.HashGenerator;
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A listener that listens for events that is related to user upsert
 *
 * @author karthick.v
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_UPSERT)
public class UserUpsertHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param data Data to be consumed
     * @return A byte array representing the result of the consumption and processing
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject response = Json.object();
        final JsonObject userJson = Json.object(data);
        final User user = UserConverter.getInstance().convert(userJson.getLong(UserFields.ORG_ID), userJson);
        final Collection<String> violations = EntityValidator.validate(List.of(user), Objects.isNull(user.id()) ? Group.Create.class : Group.Update.class);

        if (violations.isEmpty()) {
            final UserManager userManager = UserManager.getInstance();
            final String email = user.getEmail();
            final String mobile = user.getMobile();
            final boolean isCreateEvent = userJson.getBoolean(IamFields.IS_CREATE_EVENT);
            final JsonArray jsonArray = Json.array();

            if (!isCreateEvent) {
                final Long userId = user.id();

                user.setHasActivePassword(userManager.isPasswordActive(userId));

                if (Objects.nonNull(email) && !userManager.isEmailExists(userId, email)) {
                    jsonArray.put("i18n.iam.user.email.modification.restricted");
                }

                if (Objects.nonNull(mobile) && userManager.isMobileExists(user.orgId(), userId, mobile)) {
                    jsonArray.put("i18n.iam.user.phone.already.exists");
                }
            } else {
                if (Objects.nonNull(email) && userManager.isEmailExists(email)) {
                    jsonArray.put("i18n.iam.user.email.already.exists");
                }

                if (Objects.nonNull(mobile) && userManager.isMobileExists(mobile)) {
                    jsonArray.put("i18n.iam.user.phone.already.exists");
                }
            }
            response.put(IamFields.ERRORS, jsonArray);

            if (0 == jsonArray.size()) {

                if (isCreateEvent) {
                    user.setPasswordHash(HashGenerator.getInstance().generateHash(user.getPassword()));

                    userManager.save(user);
                } else {
                    userManager.update(user);
                }
            }
        } else {
            response.put(IamFields.ERRORS, violations);
        }
        response.put(UserFields.ID, user.id());

        return response.getBytes();
    }
}

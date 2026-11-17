package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;

/**
 * A listener that listens for events that is related to fetch the user
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_USER)
public class UserFetchHandler implements EventConsumer<byte[]> {

    @Override
    public byte[] consume(final byte[] jsonBytes) {
        final JsonObject jsonObject = Json.object(jsonBytes);
        final UserManager userManager = UserManager.getInstance();
        final User user;

        if (jsonObject.containsKey(UserFields.ORG_ID) && jsonObject.containsKey(UserFields.ID)) {
            user = userManager.get(jsonObject.getLong(UserFields.ORG_ID), jsonObject.getLong(UserFields.ID));
        } else if (jsonObject.containsKey(UserFields.ID)) {
            user = userManager.get(jsonObject.getLong(UserFields.ID));
        } else if (jsonObject.containsKey(UserFields.EMAIL)) {
            if (jsonObject.containsKey(UserFields.ORG_ID)) {
                user = userManager.getUserByEmail(jsonObject.getLong(UserFields.ORG_ID), jsonObject.getString(UserFields.EMAIL));
            } else {
                user = userManager.getUserByEmail(jsonObject.getString(UserFields.EMAIL));
            }
        } else if (jsonObject.containsKey(UserFields.MOBILE)) {
            if (jsonObject.containsKey(UserFields.ORG_ID)) {
                user = userManager.getUserByMobileNumber(jsonObject.getLong(UserFields.ORG_ID), jsonObject.getString(UserFields.MOBILE));
            } else {
                user = userManager.getUserByMobileNumber(jsonObject.getString(UserFields.MOBILE));
            }
        } else {
            user = null;
        }

        return Objects.isNull(user) ? null : UserConverter.getInstance().convert(user, jsonObject.optBoolean(UserFields.PASSWORD_REQUIRED, false)).getBytes();
    }

}

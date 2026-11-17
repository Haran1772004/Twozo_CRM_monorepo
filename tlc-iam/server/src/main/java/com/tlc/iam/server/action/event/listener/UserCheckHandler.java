package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is related to check the existence of a user
 *
 * @author Selvakumar
 * @version 1.0
 */
@EventListener(name = IamEvents.CHECK_USER_EXISTENCE)
public class UserCheckHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(final byte[] jsonBytes) {
        final JsonObject jsonObject = Json.object(jsonBytes);
        final UserManager userManager = UserManager.getInstance();

        if (jsonObject.containsKey(UserFields.ID)) {
            return userManager.exists(jsonObject.getLong(UserFields.ID), jsonObject.getLong(UserFields.ORG_ID));
        } else if (jsonObject.containsKey(UserFields.EMAIL)) {
            return userManager.isActiveUserEmailExists(jsonObject.getString(UserFields.EMAIL));
        } else {
            return false;
        }
    }

}

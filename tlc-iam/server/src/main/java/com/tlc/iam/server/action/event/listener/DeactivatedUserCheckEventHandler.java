package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Collection;
import java.util.HashSet;

/**
 * A listener that listens for events used to check whether any of the specified
 * users in an organization are currently in a deactivated state.
 *
 * @author Kishore J
 * @version 1.0
 */
@EventListener(name = IamEvents.DEACTIVATED_USERS_CHECK)
public class DeactivatedUserCheckEventHandler implements EventConsumer<byte[]> {

    private final UserManager userManager;

    public DeactivatedUserCheckEventHandler() {
        userManager = UserManager.getInstance();
    }

    @Override
    public Boolean consume(final byte[] bytes) {
        final JsonObject jsonObject = Json.object(bytes);

        if (jsonObject.containsKey(UserFields.USER_IDS)) {
            final JsonArray userIdArray = jsonObject.getJsonArray(UserFields.USER_IDS);
            final int size = userIdArray.size();
            final Collection<Long> userIds = new HashSet<>(size);

            for (int index = 0; index < size; index++) {
                userIds.add(userIdArray.getLong(index));
            }
            return userManager.hasAnyDeactivatedUser(jsonObject.getLong(UserFields.ORG_ID), userIds);
        }
        return false;
    }
}


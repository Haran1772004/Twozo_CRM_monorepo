package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is related to deactivate the user
 *
 * @author AswiniN
 * @version 1.0
 */
@EventListener(name = IamEvents.USER_DEACTIVATE)
public class UserDeactivateHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(final byte[] userBytes) {
        final JsonObject jsonObject = Json.object(userBytes);

        return jsonObject.containsKey(UserFields.ID) && UserManager.getInstance().deactivate(jsonObject.getLong(UserFields.ID));
    }
}

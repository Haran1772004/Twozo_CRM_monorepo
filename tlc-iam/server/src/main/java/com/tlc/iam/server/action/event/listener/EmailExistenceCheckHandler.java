package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is used check whether an email exists or not
 *
 * @author Kishore
 * @version 1.0
 */
@EventListener(name = IamEvents.CHECK_EMAIL_EXISTENCE)
public class EmailExistenceCheckHandler implements EventConsumer<byte[]> {

    @Override
    public Boolean consume(final byte[] bytes) {
        final JsonObject jsonObject = Json.object(bytes);

        if (jsonObject.containsKey(UserFields.EMAIL)) {
            return UserManager.getInstance().isEmailAddressExists(jsonObject.getString(UserFields.EMAIL));
        }
        return false;
    }
}

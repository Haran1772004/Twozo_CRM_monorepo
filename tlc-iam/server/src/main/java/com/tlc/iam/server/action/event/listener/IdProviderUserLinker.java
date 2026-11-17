package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.server.internal.api.manager.UserManager;
import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events that is related to oauth user
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
@EventListener(name = IamEvents.LINK_ID_PROVIDER_USER)
public class IdProviderUserLinker implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     *
     * @param data Data to be consumed
     * @return A byte array representing the result of the consumption and processing
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject dataJson = Json.object(data);
        final JsonObject userJson = dataJson.getJsonObject(UserFields.USER);

        if (!userJson.containsKey(UserFields.IDP_USER_ID)) {
            final String idpUserId = dataJson.getString(UserFields.IDP_USER_ID);

            UserManager.getInstance().setIdpUserId(userJson.getLong(UserFields.ORG_ID),
                    userJson.getLong(UserFields.ID), idpUserId);
            userJson.put(UserFields.IDP_USER_ID, idpUserId);
        }

        return dataJson.getBytes();
    }
}

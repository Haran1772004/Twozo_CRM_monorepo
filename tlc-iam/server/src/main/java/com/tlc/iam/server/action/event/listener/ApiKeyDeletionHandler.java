package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.Json;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;

import com.tlc.iam.server.internal.api.manager.ApiKeyAccessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

/**
 * A listener that listens for events to deletes the API-Key.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@EventListener(name = IamEvents.DELETE_API_KEY)
public class ApiKeyDeletionHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final JsonObject response = Json.object();
        final long orgId = jsonObject.getLong(IamFields.ORG_ID);
        final long id = jsonObject.getLong(IamFields.ID);
        final ApiKeyAccessManager apiKeyAccessManager = ApiKeyAccessManager.getInstance();

        try {
            if (apiKeyAccessManager.exists(orgId, id)) {
                apiKeyAccessManager.delete(orgId, id);
                response.put(IamFields.MESSAGE, IamFields.SUCCESS);
            } else {
                response.put(IamFields.MESSAGE, IamFields.NOT_EXISTS);
            }
        } catch (ErrorCode errorCode) {
            response.put(IamFields.MESSAGE, IamFields.FAILED);
        }

        return response.getBytes();
    }
}


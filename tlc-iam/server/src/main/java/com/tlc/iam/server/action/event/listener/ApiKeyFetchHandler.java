package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.ApiKeyStore;
import com.tlc.iam.server.internal.api.manager.ApiKeyAccessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;

/**
 * A listener that listens for events to fetch the API-Key.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@EventListener(name = IamEvents.FETCH_API_KEY)
public class ApiKeyFetchHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final JsonObject response = Json.object();
        final long orgId = jsonObject.getLong(IamFields.ORG_ID);
        final long id = jsonObject.getLong(IamFields.ID);
        final ApiKeyStore apiKeyStore = ApiKeyAccessManager.getInstance().get(orgId, id);

        if (Objects.nonNull(apiKeyStore)) {
            response.put(IamFields.ACCESS_TOKEN, apiKeyStore.getApiKey());
            response.put(IamFields.EXPIRATION_TIME, apiKeyStore.getExpirationTime());
        }
        return response.getBytes();
    }
}

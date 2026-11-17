package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.crypto.HashVerifier;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.ApiKeyStore;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.UserConverter;

import com.tlc.iam.server.internal.api.manager.ApiKeyAccessManager;
import com.tlc.iam.server.internal.api.manager.UserManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import java.util.Objects;

/**
 * A listener that listens for events to authorize the API-Key.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@EventListener(name = IamEvents.API_KEY_AUTHENTICATION)
public class ApiKeyAuthenticationHandler implements EventConsumer<byte[]> {

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final String apiKey = jsonObject.getString(IamFields.ACCESS_TOKEN);
        final ApiKeyStore apiKeyStore = ApiKeyAccessManager.getInstance().getByKey(apiKey);

        if (Objects.nonNull(apiKeyStore) && Objects.equals(apiKey, apiKeyStore.getApiKey())) {
            final User user = UserManager.getInstance().get(apiKeyStore.orgId(), apiKeyStore.getUserId());

            return UserConverter.getInstance().convert(user).getBytes();
        } else {
            return Json.object().put(IamFields.ERROR, "iam.api.key.authenticate.failed").getBytes();
        }
    }
}

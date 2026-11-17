package com.tlc.iam.server.action.event.listener;

import com.tlc.commons.crypto.RandomDataGenerator;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.ApiKeyStore;
import com.tlc.iam.server.internal.api.manager.ApiKeyAccessManager;

import com.tlc.web.client.event.EventConsumer;
import com.tlc.web.client.event.EventListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Base64;

/**
 * A listener that listens for events to generate the API-Key.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@EventListener(name = IamEvents.GENERATE_API_KEY)
public class ApiKeyGenerateHandler implements EventConsumer<byte[]> {

    private static final String HMAC_SHA_256 = "HmacSHA256";

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] consume(final byte[] data) {
        final JsonObject jsonObject = Json.object(data);
        final JsonObject response = Json.object();
        final long orgId = jsonObject.getLong(IamFields.ORG_ID);
        final long id = jsonObject.getLong(IamFields.ID);
        final ApiKeyStore apiKeyStore = new ApiKeyStore(orgId, id);

        try {
            final byte[] bytes = RandomDataGenerator.getInstance().generateRandomBytes(32);
            final Mac mac = Mac.getInstance(HMAC_SHA_256);
            final String secretKey = String.format("%d%d", orgId, id);

            mac.init(new SecretKeySpec(secretKey.getBytes(), HMAC_SHA_256));
            final byte[] hash = mac.doFinal(bytes);
            final String apiKey = Base64.getEncoder().encodeToString(hash);

            apiKeyStore.setApiKey(apiKey);
            apiKeyStore.setExpirationTime(jsonObject.optLong(IamFields.EXPIRATION_TIME, Instant.now().plus(90, ChronoUnit.DAYS).toEpochMilli()));
            ApiKeyAccessManager.getInstance().save(apiKeyStore);

            response.put(IamFields.ACCESS_TOKEN, apiKey);
            response.put(IamFields.EXPIRATION_TIME, apiKeyStore.getExpirationTime());
        } catch (Exception exception) {
            response.put(IamFields.ERROR, exception.getMessage());
        }

        return response.getBytes();
    }
}

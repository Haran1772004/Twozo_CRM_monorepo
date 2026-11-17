package com.tlc.iam.client.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.iam.client.ApiKeyAccessService;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.status.IamErrorCodes;

import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

/**
 * Service for managing API key creation and retrieval within the IAM layer.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ApiKeyAccessServiceImpl implements ApiKeyAccessService {

    private ApiKeyAccessServiceImpl() {
    }

    private static class Instance {
        private static final ApiKeyAccessServiceImpl INSTANCE = new ApiKeyAccessServiceImpl();
    }

    public static ApiKeyAccessServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId      The IAM organization identifier for which the key is created.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param jsonObject    The request data.
     * @param eventCallback Callback used to return the API key creation response.
     */
    @Override
    public void generateApikey(final long iamOrgId, final long iamUserId, final JsonObject jsonObject, final EventCallback<JsonObject> eventCallback) {
        jsonObject.put(IamFields.ORG_ID, iamOrgId);
        jsonObject.put(IamFields.ID, iamUserId);

        final Event<byte[]> apiKeyGenerateEvent = new Event<>(IamEvents.GENERATE_API_KEY, jsonObject.getBytes());
        final EventCallback<byte[]> apiKeyEventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                eventCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.API_KEY_GENERATION_FAILED));
            }
        };

        EventNotifier.get().send(apiKeyGenerateEvent, apiKeyEventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId      The IAM organization identifier whose API key is being retrieved.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param eventCallback Callback used to return the API key retrieval response.
     */
    @Override
    public void fetchApiKey(final long iamOrgId, final long iamUserId, final EventCallback<JsonObject> eventCallback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(IamFields.ORG_ID, iamOrgId);
        jsonObject.put(IamFields.ID, iamUserId);

        final Event<byte[]> apiKeyFetchEvent = new Event<>(IamEvents.FETCH_API_KEY, jsonObject.getBytes());
        final EventCallback<byte[]> apiKeyEventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                eventCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.FAILED_TO_FETCH_API_KEY));
            }
        };

        EventNotifier.get().send(apiKeyFetchEvent, apiKeyEventCallback);
    }

    /**
     * {@inheritDoc}
     *
     * @param iamOrgId      The IAM organization identifier whose API key is being retrieved.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param eventCallback Callback used to return the API key retrieval response.
     */
    @Override
    public void deleteApiKey(final long iamOrgId, final long iamUserId, final EventCallback<JsonObject> eventCallback) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(IamFields.ORG_ID, iamOrgId);
        jsonObject.put(IamFields.ID, iamUserId);

        final Event<byte[]> apiKeyFetchEvent = new Event<>(IamEvents.DELETE_API_KEY, jsonObject.getBytes());
        final EventCallback<byte[]> apiKeyEventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] bytes) {
                eventCallback.onSuccess(Json.object(bytes));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                eventCallback.onFailure(ErrorCode.get(IamErrorCodes.API_KEY_DELETION_FAILED));
            }
        };

        EventNotifier.get().send(apiKeyFetchEvent, apiKeyEventCallback);
    }
}
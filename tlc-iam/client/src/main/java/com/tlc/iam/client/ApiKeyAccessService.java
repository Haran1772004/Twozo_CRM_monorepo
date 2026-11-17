package com.tlc.iam.client;

import com.tlc.commons.json.JsonObject;

import com.tlc.iam.client.internal.ApiKeyAccessServiceImpl;

import com.tlc.web.client.event.EventCallback;

/**
 * Service interface for managing API key creation and retrieval within the IAM layer.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ApiKeyAccessService {

    /**
     * Fetches the {@code ApiKeyAccessService} instance.
     *
     * @return the instance of {@code ApiKeyAccessService}.
     */
    static ApiKeyAccessService getInstance() {
        return ApiKeyAccessServiceImpl.getInstance();
    }

    /**
     * Generates a new API key for the specified organization and user.
     *
     * @param iamOrgId      The IAM organization identifier for which the key is created.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param data          The request data.
     * @param eventCallback Callback used to return the API key creation response.
     */
    void generateApikey(final long iamOrgId, final long iamUserId, final JsonObject data, final EventCallback<JsonObject> eventCallback);

    /**
     * Fetches an existing API key associated with the specified organization and user.
     *
     * @param iamOrgId      The IAM organization identifier whose API key is being retrieved.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param eventCallback Callback used to return the API key retrieval response.
     */
    void fetchApiKey(final long iamOrgId, final long iamUserId, final EventCallback<JsonObject> eventCallback);

    /**
     * Deletes an existing API key associated with the specified organization and user.
     *
     * @param iamOrgId      The IAM organization identifier whose API key is being retrieved.
     * @param iamUserId     The IAM user identifier associated with the API key.
     * @param eventCallback Callback used to return the API key retrieval response.
     */
    void deleteApiKey(final long iamOrgId, final long iamUserId, final EventCallback<JsonObject> eventCallback);
}
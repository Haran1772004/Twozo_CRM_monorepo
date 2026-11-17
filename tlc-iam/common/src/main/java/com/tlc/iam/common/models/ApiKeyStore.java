package com.tlc.iam.common.models;

import com.tlc.validator.TlcModel;

/**
 * Refers the {@code ApiKeyStore}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ApiKeyStore implements TlcModel {

    private Long id;
    private final Long orgId;
    private final Long userId;
    private Long createdTime;
    private Long updatedTime;
    private Long expirationTime;
    private String apiKey;
    private String apiKeyHash;

    public ApiKeyStore(final Long orgId, final Long userId) {
        this.orgId = orgId;
        this.userId = userId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(final Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}

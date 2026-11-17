package com.tlc.sync.service.oauth.model.request;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;

/**
 * <p>
 * Represents an abstract request to store the token
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public abstract class TokenWriteRequest extends TokenRequest {

    private String accessCode;
    private SyncUrlConfig syncUrlConfig;
    private String token;
    private Long tokenId;
    private Long nextRefreshedAt;

    protected TokenWriteRequest(final SyncServiceCategory syncServiceCategory, final String accessCode, final SyncUrlConfig syncUrlConfig, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
        this.accessCode = accessCode;
        this.syncUrlConfig = syncUrlConfig;
    }

    protected TokenWriteRequest(final SyncServiceCategory syncServiceCategory, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
    }

    public String getAccessCode() {
        return accessCode;
    }

    public SyncUrlConfig getSyncUrlConfig() {
        return syncUrlConfig;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(final Long tokenId) {
        this.tokenId = tokenId;
    }

    public Long getNextRefreshedAt() {
        return nextRefreshedAt;
    }

    public void setNextRefreshedAt(final Long nextRefreshedAt) {
        this.nextRefreshedAt = nextRefreshedAt;
    }
}

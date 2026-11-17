package com.tlc.sync.service.oauth.model.request.whatsapp;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

/**
 * <p>
 * Represents the Whatsapp token read request
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATokenReadRequest extends TokenReadRequest {

    private Long waBusinessAccountId;
    private String accessCode;
    private String accessToken;

    public WATokenReadRequest(final TokenType tokenType) {
        super(SyncServiceCategory.FACEBOOK, tokenType);
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(final String accessCode) {
        this.accessCode = accessCode;
    }

    public Long getWaBusinessAccountId() {
        return waBusinessAccountId;
    }

    public void setWaBusinessAccountId(final Long waBusinessAccountId) {
        this.waBusinessAccountId = waBusinessAccountId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }
}

package com.tlc.sync.service.oauth.model.request.whatsapp;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * <p>
 * Represents a request model used to store Whatsapp business access token
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATokenWriteRequest extends TokenWriteRequest {

    private final Long waBusinessAccountId;
    private Long nextRefreshedAt;

    public WATokenWriteRequest(final Long waBusinessAccountId, final String accessToken, final TokenType tokenType) {
        super(SyncServiceCategory.WHATSAPP, tokenType);
        this.waBusinessAccountId = waBusinessAccountId;
        super.setToken(accessToken);
    }

    public Long getNextRefreshedAt() {
        return nextRefreshedAt;
    }

    public void setNextRefreshedAt(final Long nextRefreshedAt) {
        this.nextRefreshedAt = nextRefreshedAt;
    }

    public Long getWaBusinessAccountId() {
        return waBusinessAccountId;
    }
}
package com.tlc.sync.service.oauth.model.request;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Token write requests for social related modules.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Getter
public class SocialWriteRequest extends TokenWriteRequest {

    private final Long orgId;

    public SocialWriteRequest(final Long orgId, final SyncServiceCategory syncServiceCategory,
                              final TokenRequest.TokenType tokenType, final String accessToken) {
        super(syncServiceCategory, tokenType);
        this.orgId = orgId;
        super.setToken(accessToken);
    }
}

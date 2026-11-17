package com.tlc.sync.service.oauth.model.request.instagram;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.SocialWriteRequest;
import lombok.Getter;

/**
 * <p>
 * Represents a request model used to store facebook token
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
public class InstagramTokenWriteRequest extends SocialWriteRequest {

    public InstagramTokenWriteRequest(final Long orgId, final TokenType tokenType, final String accessToken) {
        super(orgId, SyncServiceCategory.FACEBOOK, tokenType, accessToken);
    }
}
package com.tlc.sync.service.oauth.model.request.instagram;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.SocialTokenReadRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Represents the facebook token read request
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
public class InstagramTokenReadRequest extends SocialTokenReadRequest {

    public InstagramTokenReadRequest(final Long orgId, final TokenType tokenType) {
        super(orgId, SyncServiceCategory.INSTAGRAM, tokenType);
    }

    public InstagramTokenReadRequest(final TokenType tokenType) {
        super(SyncServiceCategory.INSTAGRAM, tokenType);
    }
}
package com.tlc.sync.service.oauth.model.request.facebook;

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
public class FBTokenReadRequest extends SocialTokenReadRequest {

    public FBTokenReadRequest(final Long orgId, final TokenType tokenType) {
        super(orgId, SyncServiceCategory.FACEBOOK, tokenType);
    }

    public FBTokenReadRequest(final TokenType tokenType) {
        super(SyncServiceCategory.FACEBOOK, tokenType);
    }
}
package com.tlc.sync.service.oauth.model.request;

import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Token read requests for social related modules.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
@Setter
@Getter
public class SocialTokenReadRequest extends TokenReadRequest {

    @Setter(AccessLevel.NONE)
    private Long orgId;
    private String accessCode;
    private String accessToken;
    private SyncUrlConfig syncUrlConfig;

    protected SocialTokenReadRequest(final Long orgId, final SyncServiceCategory syncServiceCategory, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
        this.orgId = orgId;
    }

    protected SocialTokenReadRequest(final SyncServiceCategory syncServiceCategory, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
    }
}

package com.tlc.sync.service.oauth.model.request.email;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.request.TokenReadRequest;

import lombok.Getter;
import lombok.Setter;

/**
 * A model that represents the email token read request. This request is used to get the refresh token
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
public class EmailTokenReadRequest extends TokenReadRequest {

    private final EmailAddress emailAddress;
    @Setter
    private String refreshToken;

    public EmailTokenReadRequest(final EmailAddress emailAddress, final SyncServiceCategory syncServiceCategory, final TokenType tokenType) {
        super(syncServiceCategory, tokenType);
        this.emailAddress = emailAddress;
    }
}
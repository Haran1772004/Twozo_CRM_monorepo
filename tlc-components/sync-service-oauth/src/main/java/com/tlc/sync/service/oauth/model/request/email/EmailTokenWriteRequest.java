package com.tlc.sync.service.oauth.model.request.email;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import com.tlc.sync.service.oauth.model.SyncUrlConfig;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;

/**
 * A model that represents the email token write request. This model is used to store the token in db
 *
 * @author Maharaja S
 * @version 1.0
 */
public class EmailTokenWriteRequest extends TokenWriteRequest {

    private EmailAddress emailAddress;

    public EmailTokenWriteRequest(final String accessCode, final SyncUrlConfig syncUrlConfig, final SyncServiceCategory syncServiceCategory,
                                  final TokenType tokenType) {
        super(syncServiceCategory, accessCode, syncUrlConfig, tokenType);
    }

    public EmailTokenWriteRequest(final SyncServiceCategory syncServiceCategory, final TokenType tokenType, final EmailAddress emailAddress) {
        super(syncServiceCategory, tokenType);
        this.emailAddress = emailAddress;
    }

    /**
     * <p>
     * Gets the {@link EmailAddress}
     * </p>
     *
     * @return {@link EmailAddress}
     */
    public EmailAddress getEmailAddress() {
        return this.emailAddress;
    }

    /**
     * <p>
     * Sets the {@link EmailAddress}
     * </p>
     *
     * @param emailAddress  Represents the {@link EmailAddress}
     */
    public void setEmailAddress(final EmailAddress emailAddress) {
        this.emailAddress = emailAddress;
    }
}

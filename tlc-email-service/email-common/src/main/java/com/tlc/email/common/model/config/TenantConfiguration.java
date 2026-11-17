package com.tlc.email.common.model.config;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.address.SyncAddressInfo;

/**
 * <p>
 * Configures specific settings for a tenant in a multi-tenant system, including email-related settings
 * unique to each tenant. It enables different tenants to have isolated configurations while inheriting
 * common email settings from the {@link EmailConfiguration}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailConfiguration
 */
public class TenantConfiguration extends EmailConfiguration {

    private String emailUserId;

    /**
     * {@inheritDoc}
     *
     * @return {@link SyncAddressInfo}
     */
    @Override
    public SyncAddressInfo getSyncEmailAddress() {
        final SyncAddressInfo syncAddress = super.getSyncEmailAddress();

        if (syncAddress.getConsentType() == ConsentType.USER_GRANTED) {
            throw ErrorCode.get(EmailErrorCodes.REQUIRED_TENANT);
        }

        return syncAddress;
    }

    public String getEmailUserId() {
        return emailUserId;
    }

    public void setEmailUserId(final String emailUserId) {
        this.emailUserId = emailUserId;
    }
}

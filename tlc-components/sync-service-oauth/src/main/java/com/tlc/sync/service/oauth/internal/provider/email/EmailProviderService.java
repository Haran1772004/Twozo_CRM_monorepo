package com.tlc.sync.service.oauth.internal.provider.email;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.provider.AbstractProviderService;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.model.provider.Provider;

/**
 * <p>
 * Provides the email provider for given mail id by using MX record
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailProviderService extends AbstractProviderService {

    private static class Instance {
        private static final EmailProviderService INSTANCE = new EmailProviderService();
    }

    /**
     * <p>
     * Gets the {@link EmailProviderService} Instance
     * </p>
     *
     * @return {@link EmailProviderService}
     */
    public static EmailProviderService getInstance() {
        return Instance.INSTANCE;
    }

    private EmailProviderService() {
    }

    /**
     * <p>
     * Gets the email provider for the given MX value
     * </p>
     *
     * @param providerMxValue Represents the mx value.
     * @return {@link Provider}
     */
    @Override
    protected Provider getProvider(final String providerMxValue) {
        final String mx = providerMxValue.toLowerCase();

        if (mx.contains(SyncServiceFields.GMAIL) || mx.contains(SyncServiceFields.GOOGLE)) {
            return EmailProvider.GOOGLE;
        } else if (mx.contains(SyncServiceFields.OUTLOOK)) {
            return EmailProvider.OUTLOOK;
        } else if (mx.contains(SyncServiceFields.ZOHO)) {
            return EmailProvider.ZOHO;
        } else if (mx.contains(SyncServiceFields.YAHOO)) {
            return EmailProvider.YAHOO;
        } else if (mx.contains(SyncServiceFields.ICLOUD)) {
            return EmailProvider.ICLOUD;
        } else if (mx.contains(SyncServiceFields.AOL)) {
            return EmailProvider.AOL;
        } else {
            throw ErrorCode.get(SyncErrorCodes.INVALID_EMAIL_PROVIDER);
        }
    }
}

package com.tlc.sync.service.oauth.internal.provider.contact;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.provider.AbstractProviderService;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.provider.ContactProvider;
import com.tlc.sync.service.oauth.model.provider.Provider;

/**
 * <p>
 * Provides the contact provider for given mail id by using MX record
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactProviderService extends AbstractProviderService {

    private static class Instance {
        private static final ContactProviderService INSTANCE = new ContactProviderService();
    }

    public static ContactProviderService getInstance() {
        return Instance.INSTANCE;
    }

    private ContactProviderService() {
    }

    /**
     * <p>
     * Gets provider for the given MX value
     * </p>
     *
     * @param providerMxValue Represents the mx value.
     */
    @Override
    protected Provider getProvider(final String providerMxValue) {

        if (providerMxValue.contains(SyncServiceFields.GMAIL)) {
            return ContactProvider.GOOGLE;
        } else if (providerMxValue.contains(SyncServiceFields.OUTLOOK)) {
            return ContactProvider.OUTLOOK;
        } else {
            throw ErrorCode.get(SyncErrorCodes.INVALID_PROVIDER);
        }
    }
}
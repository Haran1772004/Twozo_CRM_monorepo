package com.tlc.sync.service.oauth.internal.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.fields.SyncServiceFields;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.provider.Provider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

/**
 * <p>
 * Provides the provider for given mail id by using MX record
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public abstract class AbstractProviderService implements ProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProviderService.class);

    /**
     * <p>
     * Finds the provider for the given email address.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return the {@link Provider}
     */
    @Override
    public Provider findProvider(final EmailAddress emailAddress) {
        final String emailId = emailAddress.getEmailId();
        final String domainName = emailId.substring(emailId.indexOf("@") + 1);
        final String providerMxValue;

        try {
            final InitialDirContext dirContext = new InitialDirContext();
            final Attributes attribute = dirContext.getAttributes(SyncServiceFields.DNS + domainName,
                    new String[]{SyncServiceFields.MX});
            final Attribute attributeMX = attribute.get(SyncServiceFields.MX);
            providerMxValue = (String) attributeMX.get(0);
        } catch (NamingException e) {
            LOGGER.debug("Oauth: Invalid MX Provider, Trace: {}", e.getMessage());
            throw ErrorCode.get(SyncErrorCodes.JNDI_LOOKUP_FAILED);
        }

        return getProvider(providerMxValue);
    }

    /**
     * <p>
     * Gets provider for the given MX value
     * </p>
     *
     * @param providerMxValue Represents the mx value.
     * @return the {@link Provider}
     */
    protected abstract Provider getProvider(final String providerMxValue);
}
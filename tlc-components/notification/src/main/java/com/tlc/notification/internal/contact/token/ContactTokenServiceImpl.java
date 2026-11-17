package com.tlc.notification.internal.contact.token;

import com.tlc.notification.internal.contact.resolver.ContactTokenResolver;
import com.tlc.notification.model.TokenType;
import com.tlc.notification.service.contact.ContactTokenService;

/**
 * <p>
 * Service interface responsible for managing the caching of sync tokens for contact data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactTokenServiceImpl implements ContactTokenService {

    private final ContactTokenResolver contactTokenResolver;

    private ContactTokenServiceImpl() {
        this.contactTokenResolver = ContactTokenResolver.getInstance();
    }

    private static class Instance {
        private static final ContactTokenServiceImpl INSTANCE = new ContactTokenServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link ContactTokenServiceImpl} instance
     * </p>
     *
     * @return {@link ContactTokenServiceImpl} instance
     */
    public static ContactTokenServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return the sync token
     */
    @Override
    public String getSyncToken(final String emailAddress) {
        return contactTokenResolver.getSyncToken(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    @Override
    public void putSyncToken(final String emailAddress, final String token) {
        contactTokenResolver.putSyncToken(emailAddress, token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllSyncTokens() {
        contactTokenResolver.clearAllSyncTokens();
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @return the page token
     */
    @Override
    public String getPageToken(final String emailAddress) {
        return contactTokenResolver.getPageToken(emailAddress);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the page token
     */
    @Override
    public void putPageToken(final String emailAddress, final String token) {
        contactTokenResolver.putPageToken(emailAddress, token);
    }

    /**
     * {@inheritDoc}
     *
     * @param emailAddress Represents the email address
     * @param tokenName    Represents the token name
     */
    @Override
    public void clearToken(final String emailAddress, final TokenType tokenName) {
        contactTokenResolver.clearToken(emailAddress, tokenName);
    }
}

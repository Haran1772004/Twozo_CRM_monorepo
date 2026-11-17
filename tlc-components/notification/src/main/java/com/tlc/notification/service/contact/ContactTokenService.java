package com.tlc.notification.service.contact;

import com.tlc.notification.internal.contact.token.ContactTokenServiceImpl;
import com.tlc.notification.service.TokenService;
import com.tlc.notification.model.TokenType;

/**
 * <p>
 * Service interface responsible for managing the caching of sync tokens for contact data.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactTokenService extends TokenService {

    /**
     * <p>
     * Gets the instance for {@link ContactTokenService}.
     * </p>
     *
     * @return {@link ContactTokenService}
     */
    static ContactTokenService getInstance() {
        return ContactTokenServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the page token from cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return the page token
     */
    String getPageToken(final String emailAddress);

    /**
     * <p>
     * Stores the page token in cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the page token
     */
    void putPageToken(final String emailAddress, final String token);

    /**
     * <p>
     * Clears the token from cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param tokenName    Represents the token name
     */
    void clearToken(final String emailAddress, final TokenType tokenName);
}
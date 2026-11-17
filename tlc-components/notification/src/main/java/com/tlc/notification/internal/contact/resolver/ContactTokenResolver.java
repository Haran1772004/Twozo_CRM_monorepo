package com.tlc.notification.internal.contact.resolver;

import com.tlc.notification.internal.calendar.resolver.CalendarTokenResolver;
import com.tlc.notification.internal.resolver.TokenResolver;
import com.tlc.notification.model.TokenType;

/**
 * <p>
 * Provides the services to resolve the tokens used for contact synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactTokenResolver extends TokenResolver {

    /**
     * <p>
     * Gets the instance for {@link CalendarTokenResolver}.
     * </p>
     *
     * @return {@link CalendarTokenResolver}
     */
    static ContactTokenResolver getInstance() {
        return ContactTokenResolverImpl.getInstance();
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

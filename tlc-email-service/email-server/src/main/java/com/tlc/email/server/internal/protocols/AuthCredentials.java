package com.tlc.email.server.internal.protocols;

import com.tlc.email.server.internal.protocols.models.AuthType;

/**
 * <p>
 * Represents authentication credentials required for connecting to an email server.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface AuthCredentials {

    /**
     * <p>
     * Returns the type of authentication provided by these credentials.
     * </p>
     *
     * @return An {@link AuthType} representing the authentication mechanism (e.g., PLAIN, LOGIN, OAuth2).
     */
    AuthType authType();
}
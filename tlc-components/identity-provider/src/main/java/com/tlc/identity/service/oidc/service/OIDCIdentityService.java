package com.tlc.identity.service.oidc.service;

import com.tlc.identity.service.oidc.model.OIDCProviderUserData;
import com.tlc.identity.service.oidc.provider.OIDCProvider;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;

/**
 * <p>
 * Represents a generic OpenID Connect (OIDC) identity service.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public interface OIDCIdentityService {

    /**
     * <p>
     * Fetches the user data from the OIDC provider using the provided
     * authorization code.
     * </p>
     *
     * @param authorizationCode The authorization code returned by the provider after user consent.
     * @param action            The action type, e.g., SIGN_IN or SIGN_UP.
     * @return {@link OIDCProviderUserData} containing user's name, email, and subject.
     */
    OIDCProviderUserData getUserData(final String authorizationCode, final Action action);

    /**
     * <p>
     * Gets the user consent uri
     * </p>
     *
     * @param action  Represents the {@link Action}
     */
    String getUserConsentUri(final Action action);

    /**
     * <p>
     * Returns the OIDC provider associated with this service implementation.
     * </p>
     *
     * @return the respective {@link OIDCProvider}
     */
    OIDCProvider getProvider();
}

package com.tlc.identity.service.oidc.service.internal;

import com.tlc.identity.service.oidc.provider.OIDCProvider;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;

import java.util.Map;

/**
 * <p>
 * Configures Google OIDC properties for login service
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public class GoogleOIDCIdentityService extends AbstractOIDCIdentityService {

    public GoogleOIDCIdentityService(final String authority, final String responseType, final String grandType,
                                     final String clientId, final String clientSecret, final String userConsentScope,
                                     final String jwksUri, final String issuer, final String tokenEndpoint,
                                     final String userInfoEndpoint, final Map<Action, String> redirectUris) {
        super(authority, responseType, grandType, clientId, clientSecret,
                userConsentScope, jwksUri, issuer, tokenEndpoint, userInfoEndpoint, redirectUris);
    }

    /**
     * <p>
     * Gets the {@link }.
     * </p>
     *
     * @return The {@link OIDCProvider}
     */
    @Override
    public OIDCProvider getProvider() {
        return OIDCProvider.GOOGLE;
    }
}

package com.tlc.identity.service.oidc.service.internal;

import com.tlc.identity.service.oidc.provider.OIDCProvider;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;

import java.util.Map;

/**
 * <p>
 * Configures Google OIDC properties for login service
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public class MicrosoftOIDCIdentityService extends AbstractOIDCIdentityService {

    public MicrosoftOIDCIdentityService(final String authority, final String responseType, final String grandType,
                                        final String clientId, final String clientSecret, final String userConsentScope,
                                        final String jwksUri, final String issuer, final String tokenEndpoint,
                                        final String userInfoEndpoint, final Map<Action, String> redirectUris) {
        super(authority, responseType, grandType, clientId, clientSecret,
                userConsentScope, jwksUri, issuer, tokenEndpoint, userInfoEndpoint, redirectUris);
    }

    /**
     * <p>
     * Returns the OIDC provider associated with this service implementation.
     * </p>
     *
     * @return the respective {@link OIDCProvider}
     */
    @Override
    public OIDCProvider getProvider() {
        return OIDCProvider.MICROSOFT;
    }
}

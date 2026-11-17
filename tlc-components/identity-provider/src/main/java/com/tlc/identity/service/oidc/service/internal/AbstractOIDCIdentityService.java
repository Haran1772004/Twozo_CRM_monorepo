package com.tlc.identity.service.oidc.service.internal;

import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.internal.status.OIDCErrorCodes;
import com.tlc.identity.service.oidc.model.OIDCProviderUserData;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;
import com.tlc.identity.service.oidc.service.OIDCIdentityService;
import com.tlc.identity.service.oidc.internal.validator.IdTokenValidator;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import io.jsonwebtoken.Claims;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Configures OIDC properties for login and signup services
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public abstract class AbstractOIDCIdentityService implements OIDCIdentityService {

    protected final String clientId;
    protected final String jwksUri;
    protected final String issuer;
    protected final String tokenEndpoint;
    protected final String userInfoEndpoint;
    protected final Map<Action, String> redirectUris;
    protected final Map<Action, String> consentUris;
    protected final Map<String, String> tokenParams;

    private final IdTokenValidator idTokenValidator;

    protected AbstractOIDCIdentityService(final String authority, final String responseType, final String grantType,
                                          final String clientId, final String clientSecret, final String userConsentScope,
                                          final String jwksUri, final String issuer, final String tokenEndpoint,
                                          final String userInfoEndpoint, final Map<Action, String> redirectUris) {
        this.clientId = clientId;
        this.jwksUri = jwksUri;
        this.issuer = issuer;
        this.tokenEndpoint = tokenEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
        this.redirectUris = redirectUris;
        this.consentUris = getConsentUris(new StringBuilder().append(authority)
                .append("?response_type=").append(responseType)
                .append("&client_id=").append(clientId)
                .append("&scope=").append(userConsentScope)
                .append("&redirect_uri=").toString());
        this.tokenParams = Map.of(OIDCFields.GRANT_TYPE, grantType, OIDCFields.CLIENT_ID_PARAM, clientId,
                OIDCFields.CLIENT_SECRET_PARAM, clientSecret);
        this.idTokenValidator = IdTokenValidator.getInstance();
    }

    /**
     * <p>
     * Retrieves the user profile using the provided access token
     * </p>
     *
     * @param authorizationCode Represents the code to get the ID token
     * @param action            Represents the code to get the {@link Action}
     * @return the {@link OIDCProviderUserData}
     */
    @Override
    public OIDCProviderUserData getUserData(final String authorizationCode, final Action action) {
        try {
            final Map<String, String> requestParams = new HashMap<>(tokenParams);

            requestParams.put(OIDCFields.REDIRECT_URI, getUserRedirectUri(action));
            requestParams.put(OIDCFields.CODE, authorizationCode);
            final JsonObject tokenResponse = HttpUtil.makeRequest(new HttpRequest(new URL(tokenEndpoint),
                    requestParams, HttpMethod.POST, Collections.emptyMap()));
            final Claims claims = idTokenValidator.validateIdToken(
                    tokenResponse.getString(OIDCFields.ID_TOKEN), jwksUri, issuer, clientId);
            final JsonObject userInfoResponse = HttpUtil.makeRequest(new HttpRequest(
                    new URL(userInfoEndpoint), Collections.emptyMap(), HttpMethod.GET,
                    Map.of(OIDCFields.AUTHORIZATION, OIDCFields.BEARER + tokenResponse.get(OIDCFields.ACCESS_TOKEN))));

            return new OIDCProviderUserData(
                    claims.get(OIDCFields.NAME, String.class),
                    claims.get(OIDCFields.EMAIL, String.class),
                    claims.getSubject());
        } catch (final Exception exception) {
            throw ErrorCode.get(OIDCErrorCodes.ID_TOKEN_FETCH_FAILED);
        }
    }

    /**
     * <p>
     * Returns the user consent URI for a specific action.
     * </p>
     *
     * @param action The action for which the consent URI is requested.
     * @return The constructed user consent URI.
     */
    @Override
    public String getUserConsentUri(final Action action) {
        return consentUris.get(action);
    }

    /**
     * <p>
     * Returns the redirect URI used to receive the authorization code
     * from the OIDC provider after user consent.
     * </p>
     *
     * @param action The action for which the redirect URI is requested.
     * @return The action-specific redirect URI.
     */
    public String getUserRedirectUri(final Action action) {
        return redirectUris.get(action);
    }

    /**
     * <p>
     * Generates the user consent URIs for all supported actions.
     * </p>
     *
     * @param uri  The base URI used to build consent links.
     * @return A map of {@link Action} to action-specific consent URI.
     */
    private Map<Action, String> getConsentUris(final String uri) {
        final Map<Action, String> consentUris = new HashMap<>();

        for (final Action action : Action.values()) {
            consentUris.put(action, uri + redirectUris.get(action));
        }

        return consentUris;
    }
}

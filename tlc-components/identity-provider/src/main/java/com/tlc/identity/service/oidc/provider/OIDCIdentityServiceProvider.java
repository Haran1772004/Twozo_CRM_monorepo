package com.tlc.identity.service.oidc.provider;

import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.internal.status.OIDCErrorCodes;
import com.tlc.identity.service.oidc.service.OIDCIdentityService;
import com.tlc.identity.service.oidc.service.internal.GoogleOIDCIdentityService;
import com.tlc.identity.service.oidc.service.internal.MicrosoftOIDCIdentityService;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Provider service which provides the {@link OIDCIdentityService}
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public final class OIDCIdentityServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OIDCIdentityServiceProvider.class);

    private GoogleOIDCIdentityService googleOIDCIdentityService;
    private MicrosoftOIDCIdentityService microsoftOIDCIdentityService;

    private OIDCIdentityServiceProvider() {
    }

    private static class InstanceHolder {
        private static final OIDCIdentityServiceProvider INSTANCE = new OIDCIdentityServiceProvider();
    }

    public static OIDCIdentityServiceProvider getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Sets the properties based on providers
     * </p>
     *
     * @param configurations  Represents the {@link Configurations}
     */
    public void setProperties(final Configurations configurations) {
        final String responseType = configurations.getConfig(OIDCFields.RESPONSE_TYPE);
        final String grantType = configurations.getConfig(OIDCFields.TYPE);
        final String scope = configurations.getConfig(OIDCFields.SCOPE);
        final String domain = configurations.getConfig(OIDCFields.DOMAIN);
        final Configurations googleConfigurations = configurations.getNestedConfigs(OIDCFields.GOOGLE);
        final Configurations microsoftConfigurations = configurations.getNestedConfigs(OIDCFields.MICROSOFT);

        if (!googleConfigurations.isEmpty()) {
            this.googleOIDCIdentityService = new GoogleOIDCIdentityService(
                    googleConfigurations.getConfig(OIDCFields.AUTHORITY), responseType, grantType,
                    googleConfigurations.getConfig(OIDCFields.CLIENT_ID),
                    googleConfigurations.getConfig(OIDCFields.CLIENT_SECRET), scope,
                    googleConfigurations.getConfig(OIDCFields.JWKS_URL),
                    googleConfigurations.getConfig(OIDCFields.ISSUER),
                    googleConfigurations.getConfig(OIDCFields.TOKEN_URL),
                    googleConfigurations.getConfig(OIDCFields.USER_INFO_URL),
                    get(domain, googleConfigurations.getNestedConfigs(OIDCFields.REDIRECT_URL))
            );
        }
        LOGGER.info("Identity Service: The OIDC Identity Service has been initialized. Provider: Google");

        if (!microsoftConfigurations.isEmpty()) {
            this.microsoftOIDCIdentityService = new MicrosoftOIDCIdentityService(
                    microsoftConfigurations.getConfig(OIDCFields.AUTHORITY), responseType, grantType,
                    microsoftConfigurations.getConfig(OIDCFields.CLIENT_ID),
                    microsoftConfigurations.getConfig(OIDCFields.CLIENT_SECRET), scope,
                    microsoftConfigurations.getConfig(OIDCFields.JWKS_URL),
                    microsoftConfigurations.getConfig(OIDCFields.ISSUER),
                    microsoftConfigurations.getConfig(OIDCFields.TOKEN_URL),
                    microsoftConfigurations.getConfig(OIDCFields.USER_INFO_URL),
                    get(domain, microsoftConfigurations.getNestedConfigs(OIDCFields.REDIRECT_URL))
            );
        }
        LOGGER.info("Identity Service: The OIDC Identity Service has been initialized. Provider: Microsoft");
    }

    /**
     * <p>
     * Provides the identity service based on OIDC provider and action
     * </p>
     *
     * @return {@link OIDCIdentityService} instance
     */
    public OIDCIdentityService getOIDCIdentityService(final OIDCProvider provider) {
        return switch (provider) {
            case GOOGLE -> Optional.ofNullable(this.googleOIDCIdentityService).orElseThrow(() ->
                    ErrorCode.get(OIDCErrorCodes.GOOGLE_OIDC_SERVICE_NOT_INITIALIZED));
            case MICROSOFT -> Optional.ofNullable(this.microsoftOIDCIdentityService).orElseThrow(() ->
                    ErrorCode.get(OIDCErrorCodes.MICROSOFT_OIDC_SERVICE_NOT_INITIALIZED));
        };
    }

    /**
     * <p>
     * Converts a configuration object containing redirect URIs to a map
     * keyed by {@link Action}.
     * </p>
     *
     * @param domain         Represents the domain
     * @param configurations Configuration object containing redirect URIs
     * @return Map of action to redirect URI
     */
    private Map<Action, String> get(final String domain, final Configurations configurations) {
        final Map<Action, String> values = new HashMap<>();

        for (final Action action : Action.values()) {
            values.put(action, domain + configurations.getConfig(action.name().toLowerCase()));
        }

        return values;
    }
}

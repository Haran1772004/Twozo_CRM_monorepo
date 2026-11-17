package com.tlc.identity.service.oidc.provider;

import com.tlc.identity.service.oidc.internal.status.OIDCErrorCodes;
import com.tlc.identity.service.oidc.service.OIDCIdentityService;
import com.tlc.commons.code.ErrorCode;

/**
 * <p>
 * Enum that represents the OIDC provider type
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public enum OIDCProvider {

    GOOGLE("google", 1),
    MICROSOFT("microsoft", 2),
    ;

    private final String name;
    private final int id;

    OIDCProvider(final String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public OIDCIdentityService getIdentityService() {
        return OIDCIdentityServiceProvider.getInstance().getOIDCIdentityService(this);
    }

    /**
     * <p>
     * Gets the matched {@link OIDCProvider}
     * </p>
     *
     * @param providerName represents the provider name
     * @return {@link OIDCProvider}
     */
    public static OIDCProvider get(final String providerName) {
        try {
            return OIDCProvider.valueOf(providerName.toUpperCase().trim());
        } catch (final Exception exception) {
            throw ErrorCode.get(OIDCErrorCodes.INVALID_PROVIDER, providerName);
        }
    }

    /**
     * <p>
     * Gets the {@link OIDCProvider} by type
     * </p>
     *
     * @param id Represents the email provider type id
     * @return {@link OIDCProvider}
     */
    public static OIDCProvider valueOf(final short id) {
        return switch (id) {
            case 1 -> GOOGLE;
            case 2 -> MICROSOFT;
            default -> throw ErrorCode.get(OIDCErrorCodes.INVALID_PROVIDER);
        };
    }

    /**
     * <p>
     * An enum that represents the login and signup action
     * </p>
     *
     * @author  Mohammed Asif J.
     * @version 1.0
     */
    public enum Action {

        SIGNUP,
        LOGIN,
        ;
    }
}

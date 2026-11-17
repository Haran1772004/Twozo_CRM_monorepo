package com.tlc.crm.notifications.action.call;

/**
 * Builds a unique identity string for WebRTC authentication.
 *
 * <p>
 *     This utility class constructs an identity string using a predefined
 * prefix and dynamic parameters. The generated identity is used in WebRTC
 * token authentication for calls.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
final class TokenIdentityBuilder {

    private static final String IDENTITY_PREFIX = "CRM";
    private static final String DELIMITER = "_";

    private TokenIdentityBuilder() {}

    /**
     * Generates a unique identity string for WebRTC users.
     *
     * <p>
     *     This method constructs an identity by appending provided parameters
     * to a predefined prefix. It ensures each identity follows a structured
     * format required for WebRTC authentication.
     * </p>
     *
     * @param params Variable-length arguments representing identity components.
     * @return A structured identity string prefixed with "CRM".
     */
    static String getIdentity(final Object... params) {
        final StringBuilder identityBuilder = new StringBuilder(IDENTITY_PREFIX);

        for (final Object param : params) {
            identityBuilder.append(DELIMITER).append(param);
        }
        return identityBuilder.toString();
    }
}

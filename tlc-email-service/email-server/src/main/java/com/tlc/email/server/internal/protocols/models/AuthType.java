package com.tlc.email.server.internal.protocols.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Represents authentication mechanisms supported by SMTP and IMAP servers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum AuthType {

    PLAIN((short) 1),
    LOGIN((short) 2),

    //TODO Need to add the auth type support for remaining ones
    ;

    private final short id;

    AuthType(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Returns the unique identifier of the authentication type.
     * </p>
     *
     * @return The authentication type identifier.
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Resolves the authentication type for the given identifier.
     * </p>
     *
     * @param id Represents the authentication type identifier.
     * @return The resolved {@link AuthType}.
     */
    public static AuthType get(final short id) {
        return switch (id) {
            case 1 -> PLAIN;
            case 2 -> LOGIN;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SECURITY_MODE);
        };
    }

    /**
     * <p>
     * Checks whether the authentication type uses username and password.
     * </p>
     *
     * @return {@code true} if the authentication type is password-based.
     */
    public boolean isPasswordAuth() {
        return this == LOGIN || this == PLAIN;
    }
}
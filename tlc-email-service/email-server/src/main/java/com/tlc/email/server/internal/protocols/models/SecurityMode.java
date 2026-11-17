package com.tlc.email.server.internal.protocols.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Represents the security mode used when connecting to an email server.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SecurityMode {

    /**
     * <p>No encryption is applied for the connection.</p>
     */
    PLAIN((short) 1),

    /**
     * <p>Uses STARTTLS to upgrade a plain connection to a secure connection.</p>
     */
    STARTTLS((short) 2),

    /**
     * <p>Uses SSL/TLS encryption from the start of the connection.</p>
     */
    SSL_TLS((short) 3);

    private final short id;

    SecurityMode(final short value) {
        this.id = value;
    }

    public short getId() {
        return id;
    }

    public static SecurityMode get(final short id) {
        return switch (id) {
            case 1 -> PLAIN;
            case 2 -> STARTTLS;
            case 3 -> SSL_TLS;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_SECURITY_MODE);
        };
    }
}
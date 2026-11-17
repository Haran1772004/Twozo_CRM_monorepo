package com.tlc.email.server.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.server.internal.protocols.models.SecurityMode;

import java.util.Objects;

/**
 * <p>
 * Represents the email protocols supported by the system.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public enum Protocol {

    /**
     * <p>Simple Mail Transfer Protocol, primarily for sending emails.</p>
     */
    SMTP((short) 1),

    /**
     * <p>Internet Message Access Protocol, primarily for reading and managing emails on the server.</p>
     */
    IMAP((short) 2);

    private final short id;

    Protocol(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static Protocol get(final short id) {
        return switch (id) {
            case 1 -> SMTP;
            case 2 -> IMAP;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_PROTOCOL);
        };
    }

    /**
     * Get the Jakarta Mail protocol string based on the protocol and security mode
     *
     * @param securityMode the security mode to use.
     * @return the Jakarta Mail protocol string.
     */
    public String get(final SecurityMode securityMode) {
        final char secureKey = 's';

        final String baseProtocol = switch (this) {
            case SMTP -> SMTP.name().toLowerCase();
            case IMAP -> IMAP.name().toLowerCase();
        };

        if (Objects.isNull(securityMode)) {
            return baseProtocol;
        }

        if (Objects.equals(securityMode, SecurityMode.SSL_TLS)) {
            return baseProtocol + secureKey;
        }

        return baseProtocol;
    }

    public String get() {
        return get(null);
    }
}
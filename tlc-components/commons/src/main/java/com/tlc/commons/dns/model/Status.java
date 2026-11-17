package com.tlc.commons.dns.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.dns.status.DnsErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents the possible status values for an email domain or entity within the system.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum Status {

    /**
     * <p>
     * Represents a pending or unverified status.
     * </p>
     */
    PENDING((short) 1),

    /**
     * <p>
     * Represents a verified status.
     * </p>
     */
    VERIFIED((short) 2);

    private final short id;

    /**
     * <p>
     * Constructor for the Status enum.
     * </p>
     *
     * @param id The unique short identifier for this status.
     */
    Status(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Represent whether the status is verified or not.
     * </p>
     *
     * @return Whether the status is verified or not.
     */
    public boolean isVerified() {
        return Objects.equals(this, VERIFIED);
    }

    /**
     * <p>
     * Returns the short ID of this status.
     * </p>
     *
     * @return The short identifier corresponding to this status.
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Resolves a {@link Status} enum value based on the provided short ID.
     * </p>
     *
     * @param id The short identifier of the status.
     * @return The corresponding {@link Status} enum value.
     */
    public static Status get(final short id) {
        return switch (id) {
            case 1 -> PENDING;
            case 2 -> VERIFIED;
            default -> throw ErrorCode.get(DnsErrorCodes.UNSUPPORTED_DOMAIN_STATUS);
        };
    }
}
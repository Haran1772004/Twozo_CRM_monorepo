package com.tlc.crm.email.notification.email.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents the possible status values for an email domain or entity
 * within the system.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public enum DomainStatus {

    /**
     * <p>
     * Represents a newly created domain that has not yet been processed.
     * </p>
     */
    NEW((short) 1),

    /**
     * <p>
     * Represents a domain that is pending further actions (e.g., verification steps).
     * </p>
     */
    PENDING((short) 2),

    /**
     * <p>
     * Represents a domain that has been successfully verified.
     * </p>
     */
    VERIFIED((short) 3);

    private final short id;

    /**
     * <p>
     * Constructor for the DomainStatus enum.
     * </p>
     *
     * @param id The unique short identifier for this status.
     */
    DomainStatus(final short id) {
        this.id = id;
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
     * Represent the status is verified or not.
     * </p>
     *
     * @return The short identifier corresponding to this status.
     */
    public boolean isVerified() {
        return Objects.equals(this, VERIFIED);
    }

    /**
     * <p>
     * Represent the status is new or not.
     * </p>
     *
     * @return The short identifier corresponding to this status.
     */
    public boolean isNew() {
        return Objects.equals(this, NEW);
    }

    /**
     * <p>
     * Represent the status is pending or not.
     * </p>
     *
     * @return The short identifier corresponding to this status.
     */
    public boolean isPending() {
        return Objects.equals(this, PENDING);
    }

    /**
     * <p>
     * Resolves a {@link DomainStatus} enum value based on the provided short ID.
     * </p>
     *
     * @param id The short identifier of the status.
     * @return The corresponding {@link DomainStatus} enum value.
     */
    public static DomainStatus get(final short id) {
        return switch (id) {
            case 1 -> NEW;
            case 2 -> PENDING;
            case 3 -> VERIFIED;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_DOMAIN_STATUS);
        };
    }
}
package com.tlc.email.common.model.address;

import com.tlc.commons.code.ErrorCode;

import com.tlc.email.common.internal.status.EmailErrorCodes;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * To define address type in email
 * </p>
 * <p>
 * {@link #FROM}
 * {@link #TO}
 * {@link #BCC}
 * {@link #CC}
 */
public enum AddressType {

    FROM((short) 1, "from"),
    TO((short) 2, "to"),
    CC((short) 3, "cc"),
    BCC((short) 4, "bcc");

    private final short id;
    private final String type;

    AddressType(final short id, final String type) {
        this.id = id;
        this.type = type;
    }

    /**
     * <p>
     * Gets the address type id
     * </p>
     *
     * @return the address id
     */
    public short getId() {
        return this.id;
    }

    public String getType() {
        return type;
    }

    /**
     * <p>
     * Gets the value of {@link AddressType} for the given id.
     * </p>
     *
     * @param id Represents the id of {@link AddressType}
     * @return {@link AddressType}
     */
    public static AddressType get(final short id) {
        return switch (id) {
            case 1 -> FROM;
            case 2 -> TO;
            case 3 -> CC;
            case 4 -> BCC;
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_ADDRESS_TYPE);
        };
    }

    /**
     * <p>
     * Returns a collection of {@link AddressType} values that are considered recipient types.
     * </p>
     *
     * @return A collection of recipient address types.
     */
    public static Collection<AddressType> getRecipientTypes() {
        return List.of(TO, CC, BCC);
    }
}
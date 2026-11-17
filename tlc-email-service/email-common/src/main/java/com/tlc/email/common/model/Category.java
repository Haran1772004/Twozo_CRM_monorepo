package com.tlc.email.common.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Represents the supported DNS record categories for email domains.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum Category {

    /**
     * <p>
     * DomainKeys Identified Mail (DKIM) category.
     * </p>
     */
    DKIM((short) 1),

    /**
     * <p>
     * Sender Policy Framework (SPF) category.
     * </p>
     */
    SPF((short) 2),

    /**
     * <p>
     * Tracking-related DNS category.
     * </p>
     */
    TRACKING((short) 3);

    private final short id;

    Category(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Returns the numeric identifier associated with this category.
     * </p>
     *
     * @return the category ID
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Returns the {@link Category} corresponding to the given ID.
     * </p>
     *
     * @param id The category identifier.
     * @return The matching {@link Category}.
     */
    public static Category get(final short id) {
        return switch (id) {
            case 1 -> DKIM;
            case 2 -> SPF;
            case 3 -> TRACKING;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_DOMAIN_CATEGORY);
        };
    }
}
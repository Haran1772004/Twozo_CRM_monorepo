package com.tlc.crm.entity.audit.log.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

/**
 * <p>
 * Represents the {@link AuditSource} details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public enum AuditSource {

    COMPANY("Company", (short) 1),
    CONTACT("Contact", (short) 2),
    DEAL("Deal", (short) 3),
    PRODUCT("Product", (short) 4),
    NOTE("Note", (short) 5),
    ACTIVITY("Activity", (short) 6),
    TAG("Tag", (short) 7),
    ;

    private final String name;
    private final short id;

    AuditSource(final String name, final short id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Gets the {@link AuditSource} by type
     *
     * @param type An Integer value that represents the type of the source
     * @return the appropriate {@link AuditSource} of provide type
     */
    public static AuditSource get(final int type) {
        return switch (type) {
            case 1 -> COMPANY;
            case 2 -> CONTACT;
            case 3 -> DEAL;
            case 4 -> PRODUCT;
            case 5 -> NOTE;
            case 6 -> ACTIVITY;
            case 7 -> TAG;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE);
        };
    }

    /**
     * Gets the name of the source
     *
     * @return source name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the {@link AuditSource} by the name
     *
     * @param name Represents the source name
     * @return {@link AuditSource}
     */
    public static AuditSource get(final String name) {
        return switch (name.toLowerCase()) {
            case "company" -> COMPANY;
            case "contact" -> CONTACT;
            case "deal" -> DEAL;
            case "product" -> PRODUCT;
            case "note" -> NOTE;
            case "activity" -> ACTIVITY;
            case "tag" -> TAG;
            default -> throw ErrorCode.get(EntityErrorCodes.INVALID_SOURCE);
        };
    }
}

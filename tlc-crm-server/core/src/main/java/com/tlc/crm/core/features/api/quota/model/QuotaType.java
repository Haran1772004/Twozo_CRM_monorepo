package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * Represents the type of quota type supported by the system
 *
 * @author kishore J
 * @version 1.0
 */
public enum QuotaType {

    USER_QUOTA((short) 1),
    TEAM_QUOTA((short) 2),
    ;

    private final short id;

    QuotaType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link QuotaType} by id
     * </p>
     *
     * @param id Represents the quota type id
     * @return The {@link QuotaType}
     */
    public static QuotaType getById(final short id) {
        return switch (id) {
            case 1 -> QuotaType.USER_QUOTA;
            case 2 -> QuotaType.TEAM_QUOTA;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_QUOTA_TYPE);
        };
    }

    /**
     * Checks whether this type represents a team quota.
     *
     * @return true if this type is team quota, otherwise false
     */
    public boolean isTeamQuota() {
        return this == TEAM_QUOTA;
    }
}

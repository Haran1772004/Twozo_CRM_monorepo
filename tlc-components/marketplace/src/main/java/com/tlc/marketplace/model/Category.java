package com.tlc.marketplace.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.marketplace.status.MarketplaceErrorCodes;

/**
 * Refers the enum of {@code Category}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public enum Category {

    TASK_MANAGEMENT((short) 1, "i18n.marketplace.category.task.management"),
    TELEPHONY((short) 2, "i18n.marketplace.category.telephony");

    private final short id;
    private final String displayName;

    Category(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the display name.
     *
     * @return The display name of category.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the {@link Category} by its id.
     *
     * @param id Refers the id.
     * @return The {@link Category}.
     */
    public static Category get(final short id) {
        return switch (id) {
            case 1 -> TASK_MANAGEMENT;
            case 2 -> TELEPHONY;
            default -> throw ErrorCode.get(MarketplaceErrorCodes.INVALID_APP_CATEGORY, "i18n.marketplace.app.category.invalid");
        };
    }
}
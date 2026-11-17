package com.tlc.marketplace.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.marketplace.status.MarketplaceErrorCodes;

/**
 * Refers the enum of {@code Pricing}.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
public enum Pricing {

    FREE((short) 1, "i18n.marketplace.pricing.free"),
    PAID((short) 2, "i18n.marketplace.pricing.paid"),
    PAID_AFTER_TRIAL((short) 3, "i18n.marketplace.pricing.paid"),
    ;

    private final short id;
    private final String displayName;

    Pricing(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public short getId() {
        return id;
    }

    /**
     * Gets the display name.
     *
     * @return The display name of pricing details.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the {@link Pricing} by its id.
     *
     * @param id Refers the id.
     * @return The {@link Pricing}.
     */
    public static Pricing get(final short id) {
        return switch (id) {
            case 1 -> FREE;
            case 2 -> PAID;
            case 3 -> PAID_AFTER_TRIAL;
            default -> throw ErrorCode.get(MarketplaceErrorCodes.INVALID_APP_PRICING, "i18n.marketplace.app.pricing.invalid");
        };
    }

    /**
     * <p>
     * Checks whether the pricing type has trial period
     * </p>
     *
     * @return True if the pricing has trial period, false otherwise
     */
    public boolean hasTrialPeriod() {
        return this == PAID_AFTER_TRIAL;
    }
}
package com.tlc.crm.core.dropdown.deal;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum contains the various {@link DealLostReason} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum DealLostReason implements DefaultOption {

    OPTED_OUR_RIVAL("Opted our rival"),
    PRICE_IS_TOO_HIGH("Price is too high"),
    JUNK_LEAD("Junk Lead"),
    NOT_INTERESTED("Not interested"),
    NO_REQUIREMENT("No Requirement"),
    NEED_ONLY_IN_FUTURE("Need only in future"),
    PRODUCT_NOT_SATISFYING("Product not satisfying"),
    NO_PROPER_FOLLOW_UP("No proper follow-up"),
    APPOINTMENT_MISSED("Appointment Missed");

    private final String name;

    DealLostReason(final String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     *
     * @return The option name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is editable, otherwise it will return false
     */
    @Override
    public boolean isEditable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return true If the option is default, otherwise it will return false
     */
    public boolean isDefault() {
        return false;
    }

    /**
     * <p>
     * Retrieves a collection of default options represented by the {@link DealLostReason} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(DealLostReason.values());
    }
}

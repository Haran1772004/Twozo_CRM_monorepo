package com.tlc.crm.core.dropdown.deal;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum contains the various the {@link WonReason} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum WonReason implements DefaultOption {

    PRODUCT_FEATURE("Product feature"),
    PRICE("Price"),
    TIMING("Timing"),
    OTHERS("Others");

    private final String name;

    WonReason(final String name) {
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
    @Override
    public boolean isDefault() {
        return false;
    }

    /**
     * <p>
     * Retrieves a collection of default options represented by the {@link WonReason} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(WonReason.values());
    }
}

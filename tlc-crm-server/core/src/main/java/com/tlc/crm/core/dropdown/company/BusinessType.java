package com.tlc.crm.core.dropdown.company;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum which contains the {@link BusinessType} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum BusinessType implements DefaultOption {

    ANALYST("Analyst"),
    COMPETITOR("Competitor"),
    CUSTOMER("Customer"),
    DISTRIBUTOR("Distributor"),
    INTEGRATOR("Integrator"),
    INVESTOR("Investor"),
    OTHER("Other"),
    PARTNER("Partner"),
    PRESS("Press"),
    PROSPECT("Prospect"),
    RESELLER("Reseller"),
    SUPPLIER("Supplier"),
    VENDOR("Vendor");

    private final String name;

    BusinessType(final String name) {
        this.name = name;
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
     * {@inheritDoc}
     *
     * @return The option name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Retrieves a collection of default options represented by the {@link BusinessType} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(BusinessType.values());
    }
}
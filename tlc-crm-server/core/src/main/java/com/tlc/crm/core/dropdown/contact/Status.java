package com.tlc.crm.core.dropdown.contact;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum which contains the {@link Status} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum Status implements DefaultOption {

    LEAD("Lead"),
    SALES_QUALIFIED_LEAD("Sales qualified lead"),
    CUSTOMER("Customer");

    private final String name;

    Status(final String name) {
        this.name = name;
    }

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
     * Retrieves a collection of default options represented by the {@link Status} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public Collection<Status> getOptions() {
        return Arrays.asList(Status.values());
    }
}

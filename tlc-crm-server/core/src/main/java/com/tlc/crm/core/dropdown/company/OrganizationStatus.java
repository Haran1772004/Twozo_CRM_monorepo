package com.tlc.crm.core.dropdown.company;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum contains the various  the {@link OrganizationStatus} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum OrganizationStatus implements DefaultOption {

    ACQUIRED("Acquired"),
    ACTIVE("Active"),
    MARKET_FAILED("Market Failed"),
    PROJECT_CANCELLED("Project Cancelled"),
    SHUTDOWN("Shutdown");

    private final String name;

    OrganizationStatus(final String name) {
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
     * <p>
     * Retrieves a collection of default options represented by the {@link OrganizationStatus} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(OrganizationStatus.values());
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
}

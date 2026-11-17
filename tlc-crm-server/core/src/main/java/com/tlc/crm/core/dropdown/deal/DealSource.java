package com.tlc.crm.core.dropdown.deal;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * An enum contains the various {@link DealSource} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum DealSource implements DefaultOption {

    REFERRAL("Referral"),
    DIRECT("Direct"),
    PAID_SEARCH("Paid Search"),
    BLOGS("Blogs"),
    SOCIAL_MEDIA("Social Media"),
    DISPLAY_ADS("Display Ads"),
    EVENTS("Events"),
    WEBINAR("Webinar");

    private final String name;

    DealSource(final String name) {
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
     * Retrieves a collection of default options represented by the {@link DealSource} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DealSource> getOptions() {
        return Arrays.asList(DealSource.values());
    }
}

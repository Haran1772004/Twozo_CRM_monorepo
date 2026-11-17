package com.tlc.crm.entity.dropdown;

import java.util.Arrays;
import java.util.Collection;

/**
 * Holds the various the {@link DealSourceStaticOption} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum DealSourceStaticOption implements DefaultOption {

    WEB("i18n.deal.source.option.web"),
    ORGANIC_SEARCH("i18n.deal.source.option.organicSearch"),
    EMAIL("i18n.deal.source.option.email"),
    PHONE("i18n.deal.source.option.phone"),
    CHAT("i18n.deal.source.option.chat"),
    WEB_FORM("i18n.deal.source.option.webForm");

    private final String name;

    DealSourceStaticOption(final String name) {
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
        return false;
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
     * Retrieves a collection of default options represented by the {@link DealSourceStaticOption} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DealSourceStaticOption> getOptions() {
        return Arrays.asList(DealSourceStaticOption.values());
    }
}
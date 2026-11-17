package com.tlc.crm.core.dropdown.contact;

import com.tlc.crm.entity.dropdown.DefaultOption;

import java.util.Arrays;
import java.util.Collection;

/**
 * Holds the various the {@link ContactSourceStaticOption} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ContactSourceStaticOption implements DefaultOption {

    WEB("i18n.contact.source.option.web"),
    ORGANIC_SEARCH("i18n.contact.source.option.organicSearch"),
    EMAIL("i18n.contact.source.option.email"),
    PHONE("i18n.contact.source.option.phone"),
    CHAT("i18n.contact.source.option.chat"),
    WEB_FORM("i18n.contact.source.option.webForm");

    private final String name;

    ContactSourceStaticOption(final String name) {
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
     * Retrieves a collection of default options represented by the {@link ContactSourceStaticOption} enum values.
     * </p>
     *
     * @return The collection of {@link DefaultOption}
     */
    public static Collection<DefaultOption> getOptions() {
        return Arrays.asList(ContactSourceStaticOption.values());
    }
}

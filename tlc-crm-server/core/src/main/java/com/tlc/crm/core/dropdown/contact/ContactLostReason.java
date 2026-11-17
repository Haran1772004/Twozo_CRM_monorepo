package com.tlc.crm.core.dropdown.contact;

import com.tlc.crm.entity.dropdown.DefaultOption;

/**
 * <p>
 * An enum contains the various {@link ContactLostReason} data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum ContactLostReason implements DefaultOption {

    JUNK_LEAD("Junk Lead", true),
    NOT_ABLE_TO_REACH("Not able to reach"),
    NOT_INTERESTED("Not interested"),
    BUDGET("Budget"),
    LOST_OF_COMPETITOR("Lost of competitor"),
    ;

    private final String name;
    private final boolean isDefault;

    ContactLostReason(final String name) {
        this(name, false);
    }

    ContactLostReason(final String name, final boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
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
        return isDefault;
    }
}

package com.tlc.crm.entity.models.menu;

import java.util.Objects;

/**
 * <p>
 * Represents {@link DefaultMenu}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum DefaultMenu {

    HOME("Home"),
    CONTACTS("Contacts"),
    COMPANIES("Companies"),
    DEALS("Deals"),
    ACTIVITIES("Activities"),
    PRODUCT("Product"),
    ANALYTICS("Analytics");

    private final String name;

    DefaultMenu(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * <p>
     * Gets the name of the menu
     * </p>
     *
     * @return the name of menu
     */
    public String getName() {
        return this.name;
    }
}

package com.tlc.crm.notifications.api.call.model;

import java.util.Objects;

/**
 * Represents a country with a name and an optional ID.
 * <p>
 * This class is used to store country-related information in the DB.
 * The country name is immutable, while the ID can be set dynamically.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Country {

    private Long id;
    private String code;
    private String name;

    public Country() {

    }

    public Country(final String name, final String code) {
        this.name = name;
        this.code = code;
    }

    /**
     * Provides the unique ID of the country.
     *
     * @return The country ID, or {@code null} if not set.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique ID of the country.
     *
     * @param id The country ID to set.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Provides the code of the country.
     *
     * @return The country code, or {@code null} if not set.
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the code of the country.
     *
     * @param code The country code to set.
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * Sets the name of the country.
     *
     * @param name The country name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Provides the name of the country.
     *
     * @return The country name.
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final Country country = (Country) object;

        return Objects.equals(id, country.id) && Objects.equals(code, country.code) && Objects.equals(name, country.name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, code, name);
    }
}

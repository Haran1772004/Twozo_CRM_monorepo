package com.tlc.crm.notifications.api.call.model;

import java.util.Objects;

/**
 * Represents a state or region within a country.
 * <p>
 * This class stores the state's name, an optional unique ID, and the associated {@link Country}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class State {

    private Long id;
    private String name;
    private Country country;

    public State() {

    }

    public State(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the unique ID of the state.
     *
     * @return The state ID, or {@code null} if not set.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique ID of the state.
     *
     * @param id The state ID to set.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Retrieves the associated country of this state.
     *
     * @return The {@link Country} object, or {@code null} if not set.
     */
    public Country getCountry() {
        return country;
    }

    /**
     * Sets the associated country for this state.
     *
     * @param country The {@link Country} object to set.
     */
    public void setCountry(final Country country) {
        this.country = country;
    }

    /**
     * Sets the name of the state.
     *
     * @param name The state name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the state.
     *
     * @return The state name.
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
        final State state = (State) object;

        return Objects.equals(id, state.id) && Objects.equals(name, state.name) && Objects.equals(country, state.country);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, country);
    }
}
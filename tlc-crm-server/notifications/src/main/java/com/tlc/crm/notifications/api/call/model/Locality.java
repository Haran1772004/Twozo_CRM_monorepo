package com.tlc.crm.notifications.api.call.model;

import java.util.Objects;

/**
 * Represents a locality (city, town, or district) within a state and country.
 * <p>
 * This class stores the locality name, an optional unique ID, and its associated {@link State} and {@link Country}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Locality {

    private Long id;
    private String name;
    private State state;
    private Country country;

    public Locality() {

    }

    public Locality(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the unique ID of the locality.
     *
     * @return The locality ID, or {@code null} if not set.
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique ID of the locality.
     *
     * @param id The locality ID to set.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Retrieves the associated state of this locality.
     *
     * @return The {@link State} object, or {@code null} if not set.
     */
    public State getState() {
        return state;
    }

    /**
     * Sets the associated state for this locality.
     *
     * @param state The {@link State} object to set.
     */
    public void setState(final State state) {
        this.state = state;
    }

    /**
     * Retrieves the associated country of this locality.
     *
     * @return The {@link Country} object, or {@code null} if not set.
     */
    public Country getCountry() {
        return country = Objects.isNull(country) && Objects.nonNull(state) ? state.getCountry() : country;
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
     * Retrieves the name of the locality.
     *
     * @return The locality name.
     */
    public String getName() {
        return name = Objects.isNull(name) ? state.getName() : name;
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
        final Locality locality = (Locality) object;

        return Objects.equals(id, locality.id) && Objects.equals(name, locality.name) && Objects.equals(state, locality.state)
                && Objects.equals(country, locality.country);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name, state, country);
    }
}
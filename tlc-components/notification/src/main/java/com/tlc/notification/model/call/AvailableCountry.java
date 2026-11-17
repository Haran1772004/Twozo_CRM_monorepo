package com.tlc.notification.model.call;

import java.util.Objects;

/**
 * Represents a country with a name, code, and its associated states.
 * Provides methods to add and merge states.
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableCountry extends AvailableResource<AvailableCountry> {

    private final String code;
    private final AvailableStates availableStates;

    public AvailableCountry(final String name, final String code) {
        super(name);
        this.code = code;
        this.availableStates = new AvailableStates();
    }

    /**
     * Gets the name of the country.
     *
     * @return The country name.
     */
    public String getName() {
        return identity;
    }

    /**
     * Gets the country code.
     *
     * @return The country code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Adds a state to this country.
     *
     * @param availableState The state to add.
     */
    public void addAvailableState(final AvailableState availableState) {
        this.availableStates.add(availableState);
    }

    /**
     * Gets the states available in this country.
     *
     * @return A collection of available states.
     */
    public AvailableStates getAvailableStates() {
        return availableStates;
    }

    /**
     * Merges another AvailableCountry's states into this one.
     *
     * @param availableCountry The country to merge from.
     */
    @Override
    public void merge(final AvailableCountry availableCountry) {
        this.availableStates.set(availableCountry.getAvailableStates().getAll());
    }

    /**
     * Checks if two AvailableCountry objects are equal based on their name and code.
     *
     * @param object The object to compare with.
     * @return True if the countries are equal, otherwise false.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AvailableCountry availableCountry = (AvailableCountry) object;

        return Objects.equals(identity, availableCountry.identity) && Objects.equals(code, availableCountry.code);
    }

    /**
     * Generates a hash code for the country based on its name and code.
     *
     * @return The generated hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(identity, code);
    }
}

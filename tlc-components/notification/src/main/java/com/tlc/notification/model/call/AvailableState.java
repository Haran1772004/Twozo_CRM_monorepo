package com.tlc.notification.model.call;

import java.util.Objects;

/**
 * Represents a state within a country, containing available localities.
 * This class provides methods to manage and merge available localities.
 *
 * <p>
 * Each state is uniquely identified by its name and country association.
 * It also maintains a collection of available localities, enabling hierarchical geographic management.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableState extends AvailableResource<AvailableState> {

    private final String country;
    private final AvailableLocalities availableLocalities;

    public AvailableState(final String name, final String country) {
        super(name);
        this.country = country;
        this.availableLocalities = new AvailableLocalities();
    }

    /**
     * Gets the name of the state.
     *
     * @return The state name.
     */
    public String getName() {
        return identity;
    }

    /**
     * Gets the country associated with this state.
     *
     * @return The country name.
     */
    public String getCountry() {
        return country;
    }

    /**
     * Adds a new locality to this state.
     *
     * @param availableLocality The {@link AvailableLocality} to add.
     */
    public void addAvailableLocality(final AvailableLocality availableLocality) {
        this.availableLocalities.add(availableLocality);
    }

    /**
     * Retrieves all available localities within this state.
     *
     * @return The {@link AvailableLocalities} collection.
     */
    public AvailableLocalities getAvailableLocalities() {
        return availableLocalities;
    }

    /**
     * Merges another state's localities into this one.
     * Ensures that the current state retains its existing localities while adding new ones.
     *
     * @param availableState The state whose localities will be merged into this one.
     */
    @Override
    public void merge(final AvailableState availableState) {
        availableLocalities.set(availableState.availableLocalities.getAll());
    }

    /**
     * Checks if two AvailableState objects are equal based on their name and country.
     *
     * @param object The object to compare with.
     * @return {@code true} if both objects have the same name and country, otherwise {@code false}.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AvailableState availableState = (AvailableState) object;

        return Objects.equals(identity, availableState.identity) && Objects.equals(country, availableState.country);
    }

    /**
     * Generates a hash code based on the state name and country.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(identity, country);
    }
}

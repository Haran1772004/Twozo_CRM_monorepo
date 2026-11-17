package com.tlc.notification.model.call;

import java.util.Objects;

/**
 * Represents a locality with its name, geographical details, and available numbers.
 * This class provides methods to manage available phone numbers within the locality.
 *
 * <p>
 * A locality is associated with a {@link LocationDetails} object, which provides additional
 * geographic metadata. It also contains an {@link AvailableNumbers} collection
 * to store available phone numbers for that locality.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableLocality extends AvailableResource<AvailableLocality> {

    private final LocationDetails locationDetails;
    private final AvailableNumbers availableNumbers;

    public AvailableLocality(final String name, final LocationDetails locationDetails) {
        super(name);
        this.locationDetails = locationDetails;
        this.availableNumbers = new AvailableNumbers();
    }

    /**
     * Gets the name of the locality.
     *
     * @return The locality name.
     */
    public String getName() {
        return identity;
    }

    /**
     * Gets the location details of the locality.
     *
     * @return The {@link LocationDetails} object.
     */
    public LocationDetails getLocationDetails() {
        return locationDetails;
    }

    /**
     * Adds an available phone number to this locality.
     *
     * @param availableNumber The {@link AvailableNumber} to add.
     */
    public void addAvailableNumber(final AvailableNumber availableNumber) {
        this.availableNumbers.add(availableNumber);
    }

    /**
     * Retrieves all available phone numbers associated with this locality.
     *
     * @return The {@link AvailableNumbers} collection.
     */
    public AvailableNumbers getAvailableNumbers() {
        return availableNumbers;
    }

    /**
     * Merges the available numbers from another locality into this one.
     * This ensures that existing numbers are retained while adding new ones.
     *
     * @param availableLocality The locality whose numbers will be merged.
     */
    @Override
    public void merge(final AvailableLocality availableLocality) {
        availableNumbers.set(availableLocality.availableNumbers.getAll());
    }

    /**
     * Checks if two AvailableLocality objects are equal based on their name and location details.
     *
     * @param object The object to compare with.
     * @return {@code true} if the objects are equal, otherwise {@code false}.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AvailableLocality availableLocality = (AvailableLocality) object;

        return Objects.equals(identity, availableLocality.identity) &&
                Objects.equals(locationDetails, availableLocality.locationDetails);
    }

    /**
     * Generates a hash code based on the name and location details.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(identity, locationDetails);
    }
}

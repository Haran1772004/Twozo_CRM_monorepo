package com.tlc.notification.model.call;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import java.util.Objects;

/**
 * Represents an available phone number along with its location details.
 * This is a record type, meaning it is immutable and designed for holding data.
 *
 * <p>
 * The class provides methods for merging another instance, checking equality, and generating a hash code.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableNumber extends AvailableResource<AvailableNumber> {

    private final LocationDetails locationDetails;

    public AvailableNumber(final String number, final LocationDetails locationDetails) {
        super(number);
        this.locationDetails = locationDetails;
    }

    public String number() {
        return identity;
    }

    public LocationDetails locationDetails() {
        return locationDetails;
    }

    /**
     * Merges details from another {@link AvailableNumber} instance into this one.
     * <p>
     * Note: Since records are immutable, this method currently does nothing.
     * If merging logic is needed, consider using a factory method to return a new instance.
     * </p>
     *
     * @param availableNumber The {@link AvailableNumber} to merge from.
     */
    @Override
    public void merge(final AvailableNumber availableNumber) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION, "You can't merge the two numbers");
    }

    /**
     * Checks if two AvailableNumber instances are equal based on the phone number and location details.
     *
     * @param object The object to compare with.
     * @return {@code true} if both objects have the same number and location details, otherwise {@code false}.
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final AvailableNumber availableNumber = (AvailableNumber) object;

        return Objects.equals(identity, availableNumber.identity) &&
                Objects.equals(locationDetails, availableNumber.locationDetails);
    }

    /**
     * Generates a hash code based on the phone number and location details.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(identity, locationDetails);
    }
}

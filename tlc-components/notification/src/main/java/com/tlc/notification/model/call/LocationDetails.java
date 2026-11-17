package com.tlc.notification.model.call;

import java.util.Objects;

/**
 * Represents geographical details of a location, including country, state, and locality.
 * This class provides getter and setter methods to manage location-related attributes.
 *
 * <p>
 * The class is used to associate phone numbers or entities with a specific geographic location.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class LocationDetails {

    private String countryCode; // ISO country code (e.g., "US", "IN", "GB")
    private String country; // Full country name (e.g., "United States", "India")
    private String state; // State or region within the country
    private String locality; // City or town name

    /**
     * Gets the country code (ISO format).
     *
     * @return The country code as a string.
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code (ISO format).
     *
     * @param countryCode The country code to set.
     */
    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Gets the full country name.
     *
     * @return The country name.
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the full country name.
     *
     * @param country The country name to set.
     */
    public void setCountry(final String country) {
        this.country = country;
    }

    /**
     * Gets the state or region name.
     *
     * @return The state name.
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the state or region name.
     *
     * @param state The state name to set.
     */
    public void setState(final String state) {
        this.state = state;
    }

    /**
     * Gets the locality (city or town).
     *
     * @return The locality name.
     */
    public String getLocality() {
        return locality;
    }

    /**
     * Sets the locality (city or town).
     *
     * @param locality The locality name to set.
     */
    public void setLocality(final String locality) {
        this.locality = locality;
    }

    /**
     * Checks if two LocationDetails objects are equal based on country code, state, and locality.
     *
     * @param object The object to compare with.
     * @return {@code true} if both objects have the same attributes, otherwise {@code false}.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        LocationDetails locationDetails = (LocationDetails) object;

        return Objects.equals(countryCode, locationDetails.countryCode) &&
                Objects.equals(state, locationDetails.state) &&
                Objects.equals(locality, locationDetails.locality);
    }

    /**
     * Generates a hash code based on the country code, state, and locality.
     *
     * @return The computed hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(countryCode, state, locality);
    }
}

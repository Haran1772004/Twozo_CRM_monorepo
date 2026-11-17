package com.tlc.l10n.model.number;

import com.tlc.l10n.model.country.Country;

import java.util.Objects;

/**
 * A wrapper class to represent phone number details such as the national number and country.
 *
 * <p>This class stores the parsed phone number components including the country and
 * national number, providing methods to retrieve formatted phone number strings.</p>
 *
 * <p>Intended for localization and formatting features in international applications.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumber {

    private final Country country;
    private final String countryCode;
    private final String nationalNumber;

    /**
     * Constructs a PhoneNumber with the specified country and national number.
     *
     * @param country        the country associated with this phone number (may be null)
     * @param nationalNumber the national number without country code
     */
    PhoneNumber(final Country country, final String nationalNumber) {
        this.country = country;
        this.countryCode = Objects.isNull(country) ? null : country.getCode();
        this.nationalNumber = nationalNumber;
    }

    /**
     * Resolves the country associated with this phone number.
     *
     * @return a {@link Country} instance, or null if not found
     */
    public Country getCountry() {
        return country;
    }

    /**
     * Checks whether the phone number has a valid country code.
     *
     * @return {@code true} if the phone number has a country code, {@code false} otherwise.
     */
    public boolean hasCountryCode() {
        return Objects.nonNull(countryCode) && !countryCode.isEmpty();
    }

    /**
     * <p>
     * Fetches the phone number as a formatted string by concatenating the country code
     * and the national number without any special characters or separators.
     * </p>
     *
     * <p>For example, if the phone number is +91 9788971797, this method returns "919788971797".</p>
     *
     * @return the formatted phone number string consisting of country code and national number
     */
    public String getNumberWithCountryCode() {
        if (hasCountryCode()) {
            return countryCode + nationalNumber;
        }

        return nationalNumber;
    }

    /**
     * Retrieves the national number part (without country code) from the phone number.
     *
     * @return the national number as a string
     */
    public String getNumber() {
        return nationalNumber;
    }

    /**
     * Returns the country calling code (without '+' prefix).
     *
     * @return the country code string (e.g., "91" for India), or null if not present
     */
    public String getCountryCode() {
        return countryCode;
    }
}

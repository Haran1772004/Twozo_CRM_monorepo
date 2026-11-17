package com.tlc.notification.model.call;

/**
 * Represents a collection of available countries for phone call services.
 * This class extends {@code AvailableResources<AvailableCountry>} and is
 * used to store and manage a list of available countries.
 *
 * @author Sathish
 * @version 1.0
 */
public class AvailableCountries extends AvailableResources<AvailableCountry> {

    public static AvailableCountries empty() {
        return new AvailableCountries();
    }
}

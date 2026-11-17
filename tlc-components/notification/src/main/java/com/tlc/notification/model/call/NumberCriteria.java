package com.tlc.notification.model.call;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents criteria for selecting phone numbers based on location, type, and capabilities.
 * This class allows filtering numbers based on voice and SMS enablement and supports pagination.
 *
 * <p>
 * The class provides methods to retrieve number types in a sorted order, filter by criteria, and manage pagination.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class NumberCriteria {

    private String containsCriteria;
    private LocationDetails locationDetails; // Location details for filtering numbers
    private Collection<NumberType> numberTypes; // Collection of number types to be considered
    private PageDetails pageDetails; // Pagination details for fetching numbers
    private boolean needVoiceEnabledNumber; // Flag to indicate if voice-enabled numbers are required
    private boolean needSmsEnabledNumber; // Flag to indicate if SMS-enabled numbers are required

    public NumberCriteria() {
        pageDetails = new PageDetails(NumberType.LOCAL, null);
    }

    /**
     * Gets the criteria to fetch the numbers.
     *
     * @return Contains criteria.
     */
    public String getContainsCriteria() {
        return containsCriteria;
    }

    /**
     * Sets the contains criteria for filtering available numbers.
     *
     * @param containsCriteria Holds the criteria to fetch the numbers contains the given criteria.
     */
    public void setContainsCriteria(final String containsCriteria) {
        this.containsCriteria = containsCriteria;
    }

    /**
     * Gets the location details associated with the number criteria.
     *
     * @return The {@link LocationDetails} object.
     */
    public LocationDetails getLocationDetails() {
        return locationDetails;
    }

    /**
     * Sets the location details for filtering available numbers.
     *
     * @param locationDetails The {@link LocationDetails} to set.
     */
    public void setLocationDetails(final LocationDetails locationDetails) {
        this.locationDetails = locationDetails;
    }

    /**
     * Adds the number type that should be considered.
     *
     * @param numberType A {@link NumberType} values.
     */
    public void addNumberType(final NumberType numberType) {
        numberTypes = Objects.isNull(numberTypes) ? new ArrayList<>() : numberTypes;

        numberTypes.add(numberType);
    }

    /**
     * Sets the number types that should be considered.
     *
     * @param numberTypes A collection of {@link NumberType} values.
     */
    public void setNumberTypes(final Collection<NumberType> numberTypes) {
        this.numberTypes = numberTypes;
    }

    /**
     * Retrieves the list of available number types sorted by their predefined order.
     *
     * @return A sorted collection of {@link NumberType}.
     */
    public Collection<NumberType> getNumberTypes() {
        return Objects.isNull(numberTypes) ? Collections.emptyList() : numberTypes.stream().sorted(Comparator.comparing(NumberType::getOrder)).toList();
    }

    /**
     * Retrieves number types that are of equal or higher priority than the given number type.
     *
     * @param fromNumberType The reference {@link NumberType} to filter from.
     * @return A sorted collection of {@link NumberType} filtered by priority.
     */
    public Collection<NumberType> getNumberTypesFrom(final NumberType fromNumberType) {
        return Objects.isNull(numberTypes) ? Collections.emptyList() : numberTypes.stream()
                .filter(numberType -> numberType.getOrder() >= fromNumberType.getOrder())
                .sorted(Comparator.comparing(NumberType::getOrder))
                .toList();
    }

    /**
     * Gets the pagination details associated with the number criteria.
     *
     * @return The {@link PageDetails} object.
     */
    public PageDetails getPageDetails() {
        return pageDetails;
    }

    /**
     * Sets pagination details for fetching phone numbers.
     *
     * @param pageDetails The {@link PageDetails} object to set.
     */
    public void setPageDetails(final PageDetails pageDetails) {
        this.pageDetails = pageDetails;
    }

    /**
     * Sets whether only voice-enabled numbers should be fetched.
     *
     * @param needVoiceEnabledNumber {@code true} if voice-enabled numbers are required, otherwise {@code false}.
     */
    public void setNeedVoiceEnabledNumber(final boolean needVoiceEnabledNumber) {
        this.needVoiceEnabledNumber = needVoiceEnabledNumber;
    }

    /**
     * Checks if the criteria require voice-enabled numbers.
     *
     * @return {@code true} if voice-enabled numbers are required, otherwise {@code false}.
     */
    public boolean needVoiceEnabledNumber() {
        return needVoiceEnabledNumber;
    }

    /**
     * Sets whether only SMS-enabled numbers should be fetched.
     *
     * @param needSmsEnabledNumber {@code true} if SMS-enabled numbers are required, otherwise {@code false}.
     */
    public void setNeedSmsEnabledNumber(final boolean needSmsEnabledNumber) {
        this.needSmsEnabledNumber = needSmsEnabledNumber;
    }

    /**
     * Checks if the criteria require SMS-enabled numbers.
     *
     * @return {@code true} if SMS-enabled numbers are required, otherwise {@code false}.
     */
    public boolean needSmsEnabledNumber() {
        return needSmsEnabledNumber;
    }
}

package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.NumberCriteria;

/**
 * Provides an abstraction for fetching location-based phone number details from a provider.
 * <p>
 * This interface defines methods to retrieve available states, localities, and numbers
 * based on the given {@link NumberCriteria}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface NumberReader {

    /**
     * Fetches the available states based on the given criteria.
     * <p>
     * The implementation should query the provider for available states
     * that match the specified {@link NumberCriteria}.
     * </p>
     *
     * @param numberCriteria The criteria for fetching available states.
     * @return An {@link AvailableResourcesData} containing the list of matching states.
     */
    AvailableResourcesData<AvailableState> fetchAvailableStates(final NumberCriteria numberCriteria);

    /**
     * Fetches the available localities based on the given criteria.
     * <p>
     * The implementation should query the provider for available localities
     * that match the specified {@link NumberCriteria}.
     * </p>
     *
     * @param numberCriteria The criteria for fetching available localities.
     * @return An {@link AvailableResourcesData} containing the list of matching localities.
     */
    AvailableResourcesData<AvailableLocality> fetchAvailableLocalities(final NumberCriteria numberCriteria);

    /**
     * Fetches the available phone numbers based on the given criteria.
     * <p>
     * The implementation should query the provider for available numbers
     * that match the specified {@link NumberCriteria}.
     * </p>
     *
     * @param numberCriteria The criteria for fetching available numbers.
     * @return An {@link AvailableResourcesData} containing the list of matching phone numbers.
     */
    AvailableResourcesData<AvailableNumber> fetchAvailableNumbers(final NumberCriteria numberCriteria);
}

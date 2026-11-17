package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.PageDetails;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.TollFree;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.TollFreeReader;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A reader that fetches available states, localities, and toll-free numbers
 * from Twilio's API using the {@link AbstractNumberReader}.
 * <p>
 * This class implements methods to retrieve:
 * <ul>
 *     <li>Available states that support toll-free numbers</li>
 *     <li>Available localities within a state</li>
 *     <li>Available toll-free numbers based on criteria</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractNumberReader
 */
public class TollFreeNumberReader extends AbstractNumberReader {

    public TollFreeNumberReader(final TwilioRestClient twilioRestClient) {
        super(twilioRestClient);
    }

    /**
     * Fetches available states that support toll-free numbers.
     * <p>
     * This method retrieves a list of states (regions) where Twilio offers toll-free numbers.
     * </p>
     *
     * @param numberCriteria The criteria specifying location and filtering conditions.
     * @return {@link AvailableResourcesData} containing the list of matching states.
     */
    @Override
    public AvailableResourcesData<AvailableState> fetchAvailableStates(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableState> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final String country = numberCriteria.getLocationDetails().getCountry();
        final TollFreeReader tollFreeNumberReader = buildTollFreeReader(numberCriteria);

        final Consumer<TollFree> stateLoader = tollFree -> {
            final String region = tollFree.getRegion();

            availableResourcesData.add(new AvailableState(region, country));
        };

        loadResourceDetails(country, pageDetails, tollFreeNumberReader, stateLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available localities within a state that support toll-free numbers.
     * <p>
     * This method retrieves a list of localities (cities or towns) where Twilio offers toll-free numbers.
     * </p>
     *
     * @param numberCriteria The criteria specifying location and filtering conditions.
     * @return {@link AvailableResourcesData} containing the list of matching localities.
     */
    @Override
    public AvailableResourcesData<AvailableLocality> fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableLocality> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final TollFreeReader tollFreeNumberReader = buildTollFreeReader(numberCriteria);

        final Consumer<TollFree> localityLoader = tollFree -> {
            final String locality = tollFree.getLocality();

            availableResourcesData.add(new AvailableLocality(locality, buildLocationDetails(tollFree)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, tollFreeNumberReader, localityLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available toll-free phone numbers based on the given criteria.
     * <p>
     * This method retrieves toll-free phone numbers from Twilio's API, filtering by region and locality.
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return {@link AvailableResourcesData} containing the list of available toll-free numbers.
     */
    @Override
    public AvailableResourcesData<AvailableNumber> fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableNumber> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final TollFreeReader tollFreeNumberReader = buildTollFreeReader(numberCriteria);

        final Consumer<TollFree> numberLoader = tollFree -> {
            final String number = tollFree.getPhoneNumber().getEndpoint();

            availableResourcesData.add(new AvailableNumber(number, buildLocationDetails(tollFree)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, tollFreeNumberReader, numberLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * <p>
     * Builds the {@link LocationDetails} from the {@link TollFree}.
     * </p>
     *
     * @param tollFree Represents the details of the tollFree phone number.
     * @return {@link LocationDetails}.
     */
    private LocationDetails buildLocationDetails(final TollFree tollFree) {
        final LocationDetails locationDetails = new LocationDetails();

        locationDetails.setCountry(tollFree.getIsoCountry());
        locationDetails.setState(tollFree.getRegion());
        locationDetails.setLocality(tollFree.getLocality());

        return locationDetails;
    }

    /**
     * Builds a Twilio {@link TollFreeReader} based on the filtering criteria.
     * <p>
     * The reader is configured to filter by country, state, locality, and capabilities
     * (voice and SMS enablement).
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return A configured {@link TollFreeReader} for querying Twilio's API.
     */
    private TollFreeReader buildTollFreeReader(final NumberCriteria numberCriteria) {
        final LocationDetails locationDetails = numberCriteria.getLocationDetails();
        final String state = locationDetails.getState();
        final String locality = locationDetails.getLocality();
        final String containsCriteria = numberCriteria.getContainsCriteria();

        final TollFreeReader tollFreeReader = new TollFreeReader(locationDetails.getCountryCode())
                .setVoiceEnabled(numberCriteria.needVoiceEnabledNumber())
                .setSmsEnabled(numberCriteria.needSmsEnabledNumber());

        if (Objects.nonNull(state)) {
            tollFreeReader.setInRegion(state);
        }

        if (Objects.nonNull(locality)) {
            tollFreeReader.setInLocality(locality);
        }

        if (Objects.nonNull(containsCriteria)) {
            tollFreeReader.setContains(containsCriteria);
        }
        return tollFreeReader;
    }
}

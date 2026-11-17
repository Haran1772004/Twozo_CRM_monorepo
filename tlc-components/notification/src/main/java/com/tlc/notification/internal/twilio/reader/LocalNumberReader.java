package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.PageDetails;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.Local;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.LocalReader;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fetches available states, localities, and phone numbers from Twilio using {@link AbstractNumberReader}.
 * <p>
 * This class implements methods to retrieve:
 * <ul>
 *     <li>Available states within a country</li>
 *     <li>Available localities within a state</li>
 *     <li>Available phone numbers within a locality</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractNumberReader
 */
public class LocalNumberReader extends AbstractNumberReader {

    public LocalNumberReader(final TwilioRestClient twilioRestClient) {
        super(twilioRestClient);
    }

    /**
     * Fetches available states based on the given criteria.
     * <p>
     * This method retrieves a list of states (regions) where Twilio provides local phone numbers.
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
        final LocalReader localNumberReader = buildLocalReader(numberCriteria);

        final Consumer<Local> stateLoader = local -> {
            final String region = local.getRegion();

            availableResourcesData.add(new AvailableState(region, country));
        };

        loadResourceDetails(country, pageDetails, localNumberReader, stateLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available localities within a state based on the given criteria.
     * <p>
     * This method retrieves the list of cities or towns where Twilio offers local numbers.
     * </p>
     *
     * @param numberCriteria The criteria specifying location and filtering conditions.
     * @return {@link AvailableResourcesData} containing the list of matching localities.
     */
    @Override
    public AvailableResourcesData<AvailableLocality> fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableLocality> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final LocalReader localNumberReader = buildLocalReader(numberCriteria);

        final Consumer<Local> localityLoader = local -> {
            final String locality = local.getLocality();

            availableResourcesData.add(new AvailableLocality(locality, buildLocationDetails(local)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, localNumberReader, localityLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available phone numbers based on the given criteria.
     * <p>
     * This method retrieves local phone numbers from Twilio's API, filtering by region and locality.
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return {@link AvailableResourcesData} containing the list of available numbers.
     */
    @Override
    public AvailableResourcesData<AvailableNumber> fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableNumber> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final LocalReader localNumberReader = buildLocalReader(numberCriteria);

        final Consumer<Local> numberLoader = local -> {
            final String number = local.getPhoneNumber().getEndpoint();

            availableResourcesData.add(new AvailableNumber(number, buildLocationDetails(local)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, localNumberReader, numberLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * <p>
     * Builds the {@link LocationDetails} from the {@link Local}.
     * </p>
     *
     * @param local Represents the details of the local phone number.
     * @return {@link LocationDetails}.
     */
    private LocationDetails buildLocationDetails(final Local local) {
        final LocationDetails locationDetails = new LocationDetails();

        locationDetails.setCountry(local.getIsoCountry());
        locationDetails.setState(local.getRegion());
        locationDetails.setLocality(local.getLocality());

        return locationDetails;
    }

    /**
     * Builds a Twilio {@link LocalReader} based on the filtering criteria.
     * <p>
     * The reader is configured to filter by country, state, locality, and capabilities
     * (voice and SMS enablement).
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return A configured {@link LocalReader} for querying Twilio's API.
     */
    private LocalReader buildLocalReader(final NumberCriteria numberCriteria) {
        final LocationDetails locationDetails = numberCriteria.getLocationDetails();
        final String state = locationDetails.getState();
        final String locality = locationDetails.getLocality();
        final String containsCriteria = numberCriteria.getContainsCriteria();

        final LocalReader localReader = new LocalReader(locationDetails.getCountryCode())
                .setVoiceEnabled(numberCriteria.needVoiceEnabledNumber())
                .setSmsEnabled(numberCriteria.needSmsEnabledNumber());

        if (Objects.nonNull(state)) {
            localReader.setInRegion(state);
        }

        if (Objects.nonNull(locality)) {
            localReader.setInLocality(locality);
        }

        if (Objects.nonNull(containsCriteria)) {
            localReader.setContains(containsCriteria);
        }
        return localReader;
    }
}

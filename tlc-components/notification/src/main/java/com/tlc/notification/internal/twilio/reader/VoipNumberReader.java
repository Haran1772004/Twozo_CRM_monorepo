package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.PageDetails;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.Voip;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.VoipReader;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A reader that implements the {@link AbstractNumberReader} to fetch available
 * VoIP-related location details from the Twilio provider.
 * <p>
 * This class retrieves:
 * <ul>
 *     <li>Available states that support VoIP numbers</li>
 *     <li>Available localities within a state</li>
 *     <li>Available VoIP numbers based on criteria</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractNumberReader
 */
public class VoipNumberReader extends AbstractNumberReader {

    public VoipNumberReader(final TwilioRestClient twilioRestClient) {
        super(twilioRestClient);
    }

    /**
     * Fetches available states that support VoIP numbers.
     * <p>
     * This method retrieves a list of states (regions) where Twilio offers VoIP numbers.
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
        final VoipReader voipNumberReader = buildVoipReader(numberCriteria);

        final Consumer<Voip> stateLoader = voip -> {
            final String region = voip.getRegion();

            availableResourcesData.add(new AvailableState(region, country));
        };

        loadResourceDetails(country, pageDetails, voipNumberReader, stateLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available localities within a state that support VoIP numbers.
     * <p>
     * This method retrieves a list of localities (cities or towns) where Twilio offers VoIP numbers.
     * </p>
     *
     * @param numberCriteria The criteria specifying location and filtering conditions.
     * @return {@link AvailableResourcesData} containing the list of matching localities.
     */
    @Override
    public AvailableResourcesData<AvailableLocality> fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableLocality> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final VoipReader voipNumberReader = buildVoipReader(numberCriteria);

        final Consumer<Voip> localityLoader = voip -> {
            final String locality = voip.getLocality();

            availableResourcesData.add(new AvailableLocality(locality, buildLocationDetails(voip)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, voipNumberReader, localityLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available VoIP phone numbers based on the given criteria.
     * <p>
     * This method retrieves VoIP phone numbers from Twilio's API, filtering by region and locality.
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return {@link AvailableResourcesData} containing the list of available VoIP numbers.
     */
    @Override
    public AvailableResourcesData<AvailableNumber> fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableNumber> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final VoipReader voipNumberReader = buildVoipReader(numberCriteria);

        final Consumer<Voip> numberLoader = voip -> {
            final String number = voip.getPhoneNumber().getEndpoint();

            availableResourcesData.add(new AvailableNumber(number, buildLocationDetails(voip)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, voipNumberReader, numberLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * <p>
     * Builds the {@link LocationDetails} from the {@link Voip}.
     * </p>
     *
     * @param voip Represents the details of the voip phone number.
     * @return {@link LocationDetails}.
     */
    private LocationDetails buildLocationDetails(final Voip voip) {
        final LocationDetails locationDetails = new LocationDetails();

        locationDetails.setCountry(voip.getIsoCountry());
        locationDetails.setState(voip.getRegion());
        locationDetails.setLocality(voip.getLocality());

        return locationDetails;
    }

    /**
     * Builds a Twilio {@link VoipReader} based on the filtering criteria.
     * <p>
     * The reader is configured to filter by country, state, locality, and capabilities
     * (voice and SMS enablement).
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return A configured {@link VoipReader} for querying Twilio's API.
     */
    private VoipReader buildVoipReader(final NumberCriteria numberCriteria) {
        final LocationDetails locationDetails = numberCriteria.getLocationDetails();
        final String state = locationDetails.getState();
        final String locality = locationDetails.getLocality();
        final String containsCriteria = numberCriteria.getContainsCriteria();

        final VoipReader voipReader = new VoipReader(locationDetails.getCountryCode())
                .setVoiceEnabled(numberCriteria.needVoiceEnabledNumber())
                .setSmsEnabled(numberCriteria.needSmsEnabledNumber());

        if (Objects.nonNull(state)) {
            voipReader.setInRegion(state);
        }

        if (Objects.nonNull(locality)) {
            voipReader.setInLocality(locality);
        }

        if (Objects.nonNull(containsCriteria)) {
            voipReader.setContains(containsCriteria);
        }
        return voipReader;
    }
}

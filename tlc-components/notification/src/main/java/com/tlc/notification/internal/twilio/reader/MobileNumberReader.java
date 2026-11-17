package com.tlc.notification.internal.twilio.reader;

import com.tlc.notification.internal.twilio.reader.model.AvailableResourcesData;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableState;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.PageDetails;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.Mobile;
import com.twilio.rest.api.v2010.account.availablephonenumbercountry.MobileReader;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A reader implementation that fetches available mobile numbers, localities, and states
 * from Twilio's API based on the given {@link NumberCriteria}.
 * <p>
 * This class extends {@link AbstractNumberReader} and provides implementations for:
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
public class MobileNumberReader extends AbstractNumberReader {

    public MobileNumberReader(final TwilioRestClient twilioRestClient) {
        super(twilioRestClient);
    }

    /**
     * Fetches available states that provide mobile numbers.
     * <p>
     * This method retrieves a list of states (regions) where Twilio offers mobile numbers.
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
        final MobileReader mobileNumberReader = buildMobileReader(numberCriteria);

        final Consumer<Mobile> stateLoader = mobile -> {
            final String region = mobile.getRegion();

            availableResourcesData.add(new AvailableState(region, country));
        };

        loadResourceDetails(country, pageDetails, mobileNumberReader, stateLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available localities within a state where Twilio offers mobile numbers.
     * <p>
     * This method retrieves the list of cities or towns providing mobile numbers.
     * </p>
     *
     * @param numberCriteria The criteria specifying location and filtering conditions.
     * @return {@link AvailableResourcesData} containing the list of matching localities.
     */
    @Override
    public AvailableResourcesData<AvailableLocality> fetchAvailableLocalities(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableLocality> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final MobileReader mobileNumberReader = buildMobileReader(numberCriteria);

        final Consumer<Mobile> localityLoader = mobile -> {
            final String locality = mobile.getLocality();

            availableResourcesData.add(new AvailableLocality(locality, buildLocationDetails(mobile)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, mobileNumberReader, localityLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * Fetches available mobile phone numbers based on the given criteria.
     * <p>
     * This method retrieves mobile phone numbers from Twilio's API, filtering by region and locality.
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return {@link AvailableResourcesData} containing the list of available numbers.
     */
    @Override
    public AvailableResourcesData<AvailableNumber> fetchAvailableNumbers(final NumberCriteria numberCriteria) {
        final AvailableResourcesData<AvailableNumber> availableResourcesData = new AvailableResourcesData<>();
        final PageDetails pageDetails = numberCriteria.getPageDetails();
        final MobileReader mobileNumberReader = buildMobileReader(numberCriteria);

        final Consumer<Mobile> numberLoader = mobile -> {
            final String number = mobile.getPhoneNumber().getEndpoint();

            availableResourcesData.add(new AvailableNumber(number, buildLocationDetails(mobile)));
        };

        loadResourceDetails(numberCriteria.getLocationDetails().getCountry(), pageDetails, mobileNumberReader, numberLoader, availableResourcesData::setNextPageUrl);
        return availableResourcesData;
    }

    /**
     * <p>
     * Builds the {@link LocationDetails} from the {@link Mobile}.
     * </p>
     *
     * @param mobile Represents the details of the mobile phone number.
     * @return {@link LocationDetails}.
     */
    private LocationDetails buildLocationDetails(final Mobile mobile) {
        final LocationDetails locationDetails = new LocationDetails();

        locationDetails.setCountry(mobile.getIsoCountry());
        locationDetails.setState(mobile.getRegion());
        locationDetails.setLocality(mobile.getLocality());

        return locationDetails;
    }

    /**
     * Builds a Twilio {@link MobileReader} based on the filtering criteria.
     * <p>
     * The reader is configured to filter by country, state, locality, and capabilities
     * (voice and SMS enablement).
     * </p>
     *
     * @param numberCriteria The criteria specifying location, number type, and capabilities.
     * @return A configured {@link MobileReader} for querying Twilio's API.
     */
    private MobileReader buildMobileReader(final NumberCriteria numberCriteria) {
        final LocationDetails locationDetails = numberCriteria.getLocationDetails();
        final String state = locationDetails.getState();
        final String locality = locationDetails.getLocality();
        final String containsCriteria = numberCriteria.getContainsCriteria();

        final MobileReader mobileReader = new MobileReader(locationDetails.getCountryCode())
                .setVoiceEnabled(numberCriteria.needVoiceEnabledNumber())
                .setSmsEnabled(numberCriteria.needSmsEnabledNumber());

        if (Objects.nonNull(state)) {
            mobileReader.setInRegion(state);
        }

        if (Objects.nonNull(locality)) {
            mobileReader.setInLocality(locality);
        }

        if (Objects.nonNull(containsCriteria)) {
            mobileReader.setContains(containsCriteria);
        }
        return mobileReader;
    }
}

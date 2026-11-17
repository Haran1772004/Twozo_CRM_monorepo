package com.tlc.crm.notifications.page;

import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;

import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.api.call.model.State;
import com.tlc.notification.model.call.AvailableCountries;
import com.tlc.notification.model.call.AvailableCountry;
import com.tlc.notification.model.call.AvailableLocalities;
import com.tlc.notification.model.call.AvailableLocality;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.NumberType;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles pagination and retrieval of phone number locations (localities) for Twilio services.
 *
 * <p>
 *     This class extends {@link PageHandler} and is responsible for fetching available localities
 * from the Twilio voice call service based on country and number criteria.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumberLocationPageHandler extends PageHandler<Locality> {

    private final VoiceCallService voiceCallService;

    public PhoneNumberLocationPageHandler(final Long pageCount) {
        super(pageCount, 250L);
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
    }

    /**
     * Fetches available localities for phone numbers from the voice call service.
     *
     * <p>
     *     This method retrieves available countries, then fetches the corresponding
     * localities for each country. The data is structured into {@link Locality} objects
     * and returned in a paginated format.
     * </p>
     *
     * @return A {@link Page} containing the collection of {@link Locality} objects.
     */
    @Override
    protected Page<Locality> fetchPageData() {
        final AvailableCountries availableCountries = voiceCallService.fetchAvailableCountries(new NumberCriteria());
        final Collection<Locality> localities = new HashSet<>();

        for (final AvailableCountry availableCountry : availableCountries) {
            final Country country = new Country(availableCountry.getName(), availableCountry.getCode());
            final NumberCriteria criteria = buildNumberCriteria(availableCountry);
            final AvailableLocalities availableLocalities = voiceCallService.fetchAvailableLocalities(criteria);

            for (final AvailableLocality availableLocality : availableLocalities) {
                final LocationDetails locationDetails = availableLocality.getLocationDetails();
                final State state = new State(locationDetails.getState());
                final Locality locality = new Locality(locationDetails.getLocality());

                state.setCountry(country);
                locality.setState(state);
                localities.add(locality);
            }
        }
        final Page<Locality> localityPage = new Page<>();

        localityPage.setData(localities);
        return localityPage;
    }

    /**
     * Returns the total number of records available.
     *
     * @return Total number of available records.
     */
    @Override
    protected long totalRecordsCount() {
        return 250;
    }

    /**
     * Builds a {@link NumberCriteria} object for fetching localities of a given country.
     *
     * @param availableCountry The {@link AvailableCountry} object containing country details.
     * @return A {@link NumberCriteria} object configured with location details and required number types.
     */
    private NumberCriteria buildNumberCriteria(final AvailableCountry availableCountry) {
        final NumberCriteria criteria = new NumberCriteria();
        final LocationDetails locationDetails = new LocationDetails();

        locationDetails.setCountry(availableCountry.getName());
        locationDetails.setCountryCode(availableCountry.getCode());
        criteria.setNeedSmsEnabledNumber(true);
        criteria.setNeedVoiceEnabledNumber(true);
        criteria.setNumberTypes(Set.of(NumberType.LOCAL));
        criteria.setLocationDetails(locationDetails);

        return criteria;
    }
}

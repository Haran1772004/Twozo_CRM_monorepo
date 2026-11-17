package com.tlc.crm.notifications.action.dropdown.call;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.crm.notifications.api.call.manager.PhoneNumberManager;
import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.api.call.model.State;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.fields.NotificationFields;

import com.tlc.notification.model.call.AvailableNumber;
import com.tlc.notification.model.call.AvailableNumbers;
import com.tlc.notification.model.call.LocationDetails;
import com.tlc.notification.model.call.NumberCriteria;
import com.tlc.notification.model.call.NumberType;
import com.tlc.notification.model.call.PageDetails;
import com.tlc.notification.service.notification.VoiceCallService;
import com.tlc.notification.service.provider.NotificationServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides a list of available phone numbers for a dropdown selection.
 * <p>
 * This class fetches available phone numbers from the {@link VoiceCallService}
 * based on the given {@link NumberCriteria} and populates an {@link OptionContainer}.
 * </p>
 * <p>
 * The dropdown list includes options with country, state, locality, and number details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@StaticDropdown(name = "availableNumberList")
public class AvailableNumberList extends AbstractDropdownAction {

    private final VoiceCallService voiceCallService;
    private final PhoneNumberManager phoneNumberManager;

    public AvailableNumberList() {
        voiceCallService = NotificationServiceProvider.get().getVoiceCallService();
        phoneNumberManager = PhoneNumberManager.getInstance();
    }

    /**
     * Fetches available phone numbers based on the request parameters.
     * <p>
     * This method retrieves available phone numbers from the voice call service and
     * populates an {@link OptionContainer} with country, state, locality, and number details.
     * </p>
     *
     * @param dropdownRequest The request containing dropdown parameters.
     * @return An {@link OptionContainer} containing number options.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final NumberCriteria criteria = getNumberCriteria(dropdownRequest);
        final AvailableNumbers availableNumbers = voiceCallService.fetchAvailableNumbers(criteria);
        final OptionContainer optionContainer = new OptionContainer(availableNumbers.getTotalAvailableResources());
        final int size = availableNumbers.size();

        if (size > 0) {
            final LocationDetails locationInfo = criteria.getLocationDetails();
            final String country = locationInfo.getCountry();
            final String countryCode = locationInfo.getCountryCode();
            final Map<String, LocationDetails> locationDetailsMap = new HashMap<>();

            for (final AvailableNumber availableNumber : availableNumbers) {
                final LocationDetails locationDetails = availableNumber.locationDetails();

                locationDetails.setCountry(country);
                locationDetails.setCountryCode(countryCode);
                locationDetailsMap.put(availableNumber.number(), locationDetails);
            }
            final String countryKey = NotificationFields.COUNTRY.getName();
            final String stateKey = NotificationFields.STATE.getName();
            final String localityKey = NotificationFields.LOCALITY.getName();
            final String numberKey = NotificationFields.NUMBER.getName();
            final String priceKey = NotificationFields.PRICE.getName();
            final Map<LocationDetails, Locality> localities = phoneNumberManager.getLocations(locationDetailsMap.values());
            final boolean isStartFromOne = 1 == dropdownRequest.getLimit().start();

            for (int index = 0; index < size; index++) {
                final AvailableNumber availableNumber = availableNumbers.get(index);
                final JsonObject jsonObject = Json.object();
                final Locality locality = localities.get(availableNumber.locationDetails());

                if (isStartFromOne && index == 0) {
                    jsonObject.put(NotificationFields.IS_DEFAULT.getName(), true);
                }
                jsonObject.put(countryKey, convertCountryIntoJson(locality.getCountry()));
                jsonObject.put(stateKey, convertStateIntoJson(locality.getState()));
                jsonObject.put(localityKey, convertLocalityIntoJson(locality));
                jsonObject.put(numberKey, availableNumber.number());
                jsonObject.put(priceKey, "$1.00");//TODO need to change this once the pricing API implementations completed.

                optionContainer.addOption(jsonObject);
            }
        }
        return optionContainer;
    }

    /**
     * <p>
     * Converts the {@link Country} object into {@link JsonObject}.
     * </p>
     *
     * @param country Holds the details of the country.
     * @return Converted {@link JsonObject}.
     */
    private JsonObject convertCountryIntoJson(final Country country) {
        final JsonObject jsonObject = Json.object();
        
        jsonObject.put(NotificationFields.ID.getName(), country.getId());
        jsonObject.put(NotificationFields.NAME.getName(), country.getName());
        jsonObject.put(NotificationFields.CODE.getName(), country.getCode());
        
        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link State} object into {@link JsonObject}.
     * </p>
     *
     * @param state Holds the details of the state.
     * @return Converted {@link JsonObject}.
     */
    private JsonObject convertStateIntoJson(final State state) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), state.getId());
        jsonObject.put(NotificationFields.NAME.getName(), state.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Locality} object into {@link JsonObject}.
     * </p>
     *
     * @param locality Holds the details of the locality.
     * @return Converted {@link JsonObject}.
     */
    private JsonObject convertLocalityIntoJson(final Locality locality) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(NotificationFields.ID.getName(), locality.getId());
        jsonObject.put(NotificationFields.NAME.getName(), locality.getName());

        return jsonObject;
    }

    /**
     * Constructs a {@link NumberCriteria} object based on the dropdown request.
     * <p>
     * This method extracts location details from the request and sets appropriate
     * filtering parameters for retrieving available phone numbers.
     * </p>
     *
     * @param dropdownRequest The request containing filtering parameters.
     * @return A configured {@link NumberCriteria} object for number filtering.
     */
    private NumberCriteria getNumberCriteria(final DropdownRequest dropdownRequest) {
        final JsonObject data = dropdownRequest.data();
        final NumberCriteria criteria = new NumberCriteria();
        final LocationDetails locationDetails = new LocationDetails();
        final String stateName = data.getString(NotificationFields.STATE_NAME.getName());
        final String localityName = data.getString(NotificationFields.LOCALITY_NAME.getName());
        final String containsCriteria = (String) dropdownRequest.getCriteria().getValue();
        final String pageUrl = data.getNullableString(NotificationFields.NEXT_PAGE_URL.getName());

        locationDetails.setCountryCode(data.getString(NotificationFields.COUNTRY_CODE.getName()));
        locationDetails.setCountry(data.getString(NotificationFields.COUNTRY_NAME.getName()));
        locationDetails.setState(Objects.equals(NotificationAttributes.ALL_STATES, stateName) ? null : stateName);
        locationDetails.setLocality(Objects.equals(NotificationAttributes.ALL_LOCALITIES, localityName) ? null : localityName);

        criteria.setPageDetails(new PageDetails(NumberType.LOCAL, pageUrl, dropdownRequest.getLimit().limit()));
        criteria.setLocationDetails(locationDetails);
        criteria.setNeedSmsEnabledNumber(true);
        criteria.setNeedVoiceEnabledNumber(true);
        criteria.addNumberType(NumberType.LOCAL);
        criteria.setContainsCriteria(containsCriteria);

        return criteria;
    }
}
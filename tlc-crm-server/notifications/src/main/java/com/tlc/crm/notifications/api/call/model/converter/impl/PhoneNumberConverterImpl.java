package com.tlc.crm.notifications.api.call.model.converter.impl;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.notifications.api.call.model.Country;
import com.tlc.crm.notifications.api.call.model.Locality;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.RecordType;
import com.tlc.crm.notifications.api.call.model.State;
import com.tlc.crm.notifications.api.call.model.converter.PhoneNumberConverter;
import com.tlc.crm.notifications.fields.NotificationFields;
import com.tlc.notification.model.telephone.TelephoneProvider;

/**
 * Implementation of {@link PhoneNumberConverter} for converting between {@link PhoneNumber} objects and {@link JsonObject}.
 * <p>
 * This class provides methods to:
 * <ul>
 *     <li>Convert a JSON representation to a {@link PhoneNumber} object.</li>
 *     <li>Convert a {@link PhoneNumber} object to its JSON representation.</li>
 *     <li>Handle location details conversion.</li>
 * </ul>
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PhoneNumberConverterImpl implements PhoneNumberConverter {

    private PhoneNumberConverterImpl() {}

    private static final class InstanceHolder {
        private static final PhoneNumberConverterImpl PHONE_NUMBER_CONVERTER = new PhoneNumberConverterImpl();
    }

    /**
     * Retrieves the singleton instance of {@link PhoneNumberConverterImpl}.
     *
     * @return The single instance of {@link PhoneNumberConverterImpl}.
     */
    public static PhoneNumberConverterImpl getInstance() {
        return InstanceHolder.PHONE_NUMBER_CONVERTER;
    }

    /**
     * Converts a {@link JsonObject} into a {@link PhoneNumber} object.
     *
     * @param jsonObject The JSON representation of the phone number.
     * @return The converted {@link PhoneNumber} object.
     */
    @Override
    public PhoneNumber convert(final Long orgId, final JsonObject jsonObject) {
        final PhoneNumber phoneNumber = new PhoneNumber();
        final String idKey = NotificationFields.ID.getName();
        final String numberKey = NotificationFields.NUMBER.getName();
        final String displayNameKey = NotificationFields.DISPLAY_NAME.getName();
        final String locationDetailsKey = NotificationFields.LOCATION_DETAILS.getName();
        final String recordTypeKey = NotificationFields.RECORD_TYPE.getName();
        final String providerKey = NotificationFields.PROVIDER.getName();

        if (jsonObject.containsKey(idKey)) {
            phoneNumber.setId(jsonObject.getLong(idKey));
        }

        if (jsonObject.containsKey(numberKey)) {
            phoneNumber.setNumber(jsonObject.getString(numberKey));
        }

        if (jsonObject.containsKey(displayNameKey)) {
            phoneNumber.setDisplayName(jsonObject.getString(displayNameKey));
        }

        if (jsonObject.containsKey(locationDetailsKey)) {
            phoneNumber.setLocality(buildLocationDetails(jsonObject.getJsonObject(locationDetailsKey)));
        }
        phoneNumber.setTelephoneProvider(jsonObject.containsKey(providerKey)
                ? TelephoneProvider.get(jsonObject.getShort(providerKey)) : TelephoneProvider.getDefaultProvider());

        phoneNumber.setOrgId(orgId);
        phoneNumber.setRecordType(jsonObject.containsKey(recordTypeKey)
                ? RecordType.get(jsonObject.getShort(recordTypeKey))
                : RecordType.RECORD_ALL_CALLS);
        phoneNumber.setActive(true);

        return phoneNumber;
    }

    /**
     * Converts a {@link PhoneNumber} object into a {@link JsonObject}.
     *
     * @param phoneNumber The {@link PhoneNumber} object to convert.
     * @return The JSON representation of the phone number.
     */
    @Override
    public JsonObject convert(final PhoneNumber phoneNumber) {
        final JsonObject numberJson = Json.object();

        numberJson.put(NotificationFields.ID.getName(), phoneNumber.id());
        numberJson.put(NotificationFields.DISPLAY_NAME.getName(), phoneNumber.getDisplayName());
        numberJson.put(NotificationFields.NUMBER.getName(), phoneNumber.getNumber());
        numberJson.put(NotificationFields.RECORD_TYPE.getName(), phoneNumber.getRecordType().getId());
        numberJson.put(NotificationFields.LOCATION_DETAILS.getName(), convertLocationDetailsToJson(phoneNumber.getLocality()));

        return numberJson;
    }

    /**
     * Converts {@link Locality} to a {@link JsonObject}.
     *
     * @param locality The location details to convert.
     * @return The JSON representation of location details.
     */
    private JsonObject convertLocationDetailsToJson(final Locality locality) {
        final JsonObject locationJson = Json.object();

        locationJson.put(NotificationFields.NAME.getName(), locality.getCountry().getName());
        locationJson.put(NotificationFields.STATE_NAME.getName(), locality.getState().getName());
        locationJson.put(NotificationFields.LOCALITY_NAME.getName(), locality.getName());

        return locationJson;
    }

    /**
     * Builds a {@link Locality} object from a {@link JsonObject}.
     *
     * @param jsonObject The JSON representation of location details.
     * @return The {@link Locality} object.
     */
    private Locality buildLocationDetails(final JsonObject jsonObject) {
        final long countryId = jsonObject.getLong(NotificationFields.COUNTRY_ID.getName());
        final long stateId = jsonObject.getLong(NotificationFields.STATE_ID.getName());
        final long localityId = jsonObject.getLong(NotificationFields.LOCALITY_ID.getName());
        final Country country = new Country();
        final State state = new State();
        final Locality locality = new Locality();

        country.setId(countryId);
        state.setId(stateId);
        state.setCountry(country);
        locality.setId(localityId);
        locality.setState(state);

        return locality;
    }
}
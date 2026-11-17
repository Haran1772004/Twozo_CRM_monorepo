package com.tlc.crm.entity.models.type.converter;

import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.l10n.model.country.Country;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.AddressManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.entity.models.type.Address;

import java.util.Map;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Address}
 *
 * @author Sundar
 * @version 1.0
 */
public class AddressConverter extends EntityConverter<Address> {

    private final AddressManager addressManager;
    private final FieldTypeService fieldTypeService;

    private static class Instance {
        private static final AddressConverter INSTANCE = new AddressConverter();
    }

    private AddressConverter() {
        addressManager = AddressManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * Gets the {@link AddressConverter} instance
     *
     * @return {@link AddressConverter}
     */
    public static AddressConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Address}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the address data
     * @return the {@link Address}
     */
    @Override
    public Address convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildAddressByPartialData(converterConfig, jsonObject)
                : buildAddress(converterConfig, jsonObject);
    }

    /**
     * Converts {@link JsonObject} into {@link Address}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the address data
     * @return the {@link Address}
     */
    private Address buildAddress(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Address address = new Address();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            address.setId(jsonObject.getLong(idKey));
        }
        setDataToAddress(converterConfig, address, jsonObject);

        return address;
    }

    /**
     * Sets the json data into {@link Address}
     *
     * @param converterConfig converter configuration
     * @param address         which contains the address data
     * @param jsonObject      which contains the address fields data
     */
    public void setDataToAddress(final ConverterConfig converterConfig, final Address address, final JsonObject jsonObject) {
        final Map<StoreSubType, FieldSubType> fieldSubTypeMap = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);

        if (Objects.nonNull(fieldSubTypeMap) && !fieldSubTypeMap.isEmpty()) {
            final String latitudeKey = fieldSubTypeMap.get(StoreSubType.LATITUDE).idAsString();
            final String longitudeKey = fieldSubTypeMap.get(StoreSubType.LONGITUDE).idAsString();
            final String countryFieldId = fieldSubTypeMap.get(StoreSubType.COUNTRY).idAsString();
            final String addressLine1FieldId = fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE1).idAsString();
            final String addressLine2FieldId = fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE2).idAsString();
            final String cityFieldId = fieldSubTypeMap.get(StoreSubType.CITY).idAsString();
            final String stateFieldId = fieldSubTypeMap.get(StoreSubType.STATE).idAsString();
            final String zipcodeFieldId = fieldSubTypeMap.get(StoreSubType.ZIPCODE).idAsString();

            address.setOrgId(converterConfig.orgId());

            if (jsonObject.containsKey(addressLine1FieldId)) {
                address.setAddressLine1(jsonObject.getNullableValue(addressLine1FieldId));
            }

            if (jsonObject.containsKey(addressLine2FieldId)) {
                address.setAddressLine2(jsonObject.getNullableValue(addressLine2FieldId));
            }

            if (jsonObject.containsKey(cityFieldId)) {
                address.setCity(jsonObject.getNullableValue(cityFieldId));
            }

            if (jsonObject.containsKey(zipcodeFieldId)) {
                address.setZipcode(jsonObject.getNullableValue(zipcodeFieldId));
            }

            if (jsonObject.containsKey(stateFieldId)) {
                address.setState(jsonObject.getNullableValue(stateFieldId));
            }

            if (jsonObject.containsKey(countryFieldId)) {
                final Number countryId = NumberUtils.parseNumber(jsonObject.getNullableValue(countryFieldId));

                if (Objects.isNull(countryId)) {
                    address.setCountry(null);
                } else {
                    address.setCountry(Country.getCountryById(countryId.intValue()));
                }
            }

            if (jsonObject.containsKey(latitudeKey)) {
                address.setLatitude(jsonObject.getNullableValue(latitudeKey));
            }

            if (jsonObject.containsKey(longitudeKey)) {
                address.setLongitude(jsonObject.getNullableValue(longitudeKey));
            }
        }
    }

    /**
     * Converts the partial data present in  {@link JsonObject} into {@link Address}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the address data
     * @return the {@link Address}
     */
    private Address buildAddressByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Address address = addressManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToAddress(converterConfig, address, jsonObject);

        return address;
    }

    /**
     * Converts {@link Address} into {@link JsonObject}
     *
     * @param address A model that holds the attributes of address
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Address address) {
        final JsonObject jsonObject = Json.object();
        final Map<StoreSubType, FieldSubType> fieldSubTypeMap = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);

        if (Objects.nonNull(fieldSubTypeMap) && !fieldSubTypeMap.isEmpty()) {
            final Country country = address.getCountry();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), address.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), address.orgId());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE1).idAsString(), address.getAddressLine1());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE2).idAsString(), address.getAddressLine2());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.STATE).idAsString(), address.getState());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.CITY).idAsString(), address.getCity());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ZIPCODE).idAsString(), address.getZipcode());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.LONGITUDE).idAsString(), address.getLongitude());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.LATITUDE).idAsString(), address.getLatitude());

            if (Objects.nonNull(country)) {
                jsonObject.put(fieldSubTypeMap.get(StoreSubType.COUNTRY).idAsString(), country.getId());
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Address} into {@link JsonObject}
     *
     * @param address         A model that holds the attributes of address
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Address address) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(address);
        } else {
            final Country country = address.getCountry();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), address.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), address.orgId());
            jsonObject.put(StoreSubType.ADDRESS_LINE1.getAliasName(), address.getAddressLine1());
            jsonObject.put(StoreSubType.ADDRESS_LINE2.getAliasName(), address.getAddressLine2());
            jsonObject.put(StoreSubType.STATE.getAliasName(), address.getState());
            jsonObject.put(StoreSubType.CITY.getAliasName(), address.getCity());
            jsonObject.put(StoreSubType.ZIPCODE.getAliasName(), address.getZipcode());
            jsonObject.put(StoreSubType.LONGITUDE.getAliasName(), address.getLongitude());
            jsonObject.put(StoreSubType.LATITUDE.getAliasName(), address.getLatitude());

            if (Objects.nonNull(country)) {
                jsonObject.put(StoreSubType.COUNTRY.getAliasName(), country.getId());
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Address} into partial {@link JsonObject}
     *
     * @param address An entity that holds the attributes of address details
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Address address) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(address)) {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), address.id());
            jsonObject.put(FieldSubTypeKeys.ADDRESS_LINE1.getName(), address.getAddressLine1());
            jsonObject.put(FieldSubTypeKeys.ADDRESS_LINE2.getName(), address.getAddressLine2());
        }

        return jsonObject;
    }

    /**
     * Converts {@link Address} into {@link JsonObject}
     *
     * @param address         The {@link Address} entity containing address-related information to be converted.
     * @param converterConfig The {@link ConverterConfig} providing configuration settings..
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToJson(final Address address, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Map<StoreSubType, FieldSubType> fieldSubTypeMap = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);

        if (Objects.nonNull(fieldSubTypeMap) && !fieldSubTypeMap.isEmpty()) {
            final Country country = address.getCountry();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), address.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), address.orgId());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE1).idAsString(), address.getAddressLine1());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ADDRESS_LINE2).idAsString(), address.getAddressLine2());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.STATE).idAsString(), address.getState());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.CITY).idAsString(), address.getCity());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.ZIPCODE).idAsString(), address.getZipcode());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.LONGITUDE).idAsString(), address.getLongitude());
            jsonObject.put(fieldSubTypeMap.get(StoreSubType.LATITUDE).idAsString(), address.getLatitude());

            if (Objects.nonNull(country)) {
                jsonObject.put(fieldSubTypeMap.get(StoreSubType.COUNTRY).idAsString(), convert(country, converterConfig));
            }
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param address         The {@link Address} entity containing address-related information to be converted.
     * @param converterConfig The {@link ConverterConfig} providing configuration settings..
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertToRestJson(final Address address, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Map<StoreSubType, FieldSubType> fieldSubTypeMap = fieldTypeService.groupSubtypesByStoreSubType(StoreType.ADDRESS);

        if (Objects.nonNull(fieldSubTypeMap) && !fieldSubTypeMap.isEmpty()) {
            final Country country = address.getCountry();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), address.id());
            jsonObject.put(StoreSubType.ADDRESS_LINE1.getApiPropertyName(), address.getAddressLine1());
            jsonObject.put(StoreSubType.ADDRESS_LINE2.getApiPropertyName(), address.getAddressLine2());
            jsonObject.put(StoreSubType.STATE.getApiPropertyName(), address.getState());
            jsonObject.put(StoreSubType.CITY.getApiPropertyName(), address.getCity());
            jsonObject.put(StoreSubType.ZIPCODE.getApiPropertyName(), address.getZipcode());
            jsonObject.put(StoreSubType.LONGITUDE.getApiPropertyName(), address.getLongitude());
            jsonObject.put(StoreSubType.LATITUDE.getApiPropertyName(), address.getLatitude());

            if (Objects.nonNull(country)) {
                jsonObject.put(StoreSubType.COUNTRY.getApiPropertyName(), convert(country, converterConfig));
            }
        }

        return jsonObject;
    }

    /**
     * Converts the specified {@code Country} object into a {@code JsonObject} using the provided
     * {@code ConverterConfig}.
     *
     * @param country         the {@code Country} object to be converted
     * @param converterConfig the {@code ConverterConfig} containing configuration details
     * @return a {@code JsonObject} representing the converted country, with fields for value and name
     */
    private JsonObject convert(final Country country, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), country.getId());
        jsonObject.put(FieldKeys.NAME.getName(), i18nService.getResolver(converterConfig.locale()).get(country.getName()));

        return jsonObject;
    }
}

package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.models.type.Phone.Type;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.model.country.Country;

import java.util.Map;
import java.util.Objects;

import static com.tlc.crm.entity.models.type.Phone.Type.WORK;

/**
 * Provides different ways to convert the data from / into {@link Phone}
 *
 * @author Sundar
 * @version 1.0
 */
public class PhoneConverter extends EntityConverter<Phone> {

    private final PhoneManager phoneManager;
    private final FieldTypeService fieldTypeService;

    private static class Instance {
        private static final PhoneConverter INSTANCE = new PhoneConverter();
    }

    private PhoneConverter() {
        phoneManager = PhoneManager.getInstance();
        fieldTypeService = FieldTypeService.getInstance();
    }

    /**
     * Gets the {@link PhoneConverter} instance
     *
     * @return {@link PhoneConverter}
     */
    public static PhoneConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Phone}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the phone data
     * @return the {@link Phone}
     */
    @Override
    public Phone convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildPhoneByPartialData(converterConfig, jsonObject)
                : buildPhone(converterConfig, jsonObject);
    }

    /**
     * Converts {@link JsonObject} into {@link Phone}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the phone data
     * @return the {@link Phone}
     */
    private Phone buildPhone(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Phone phone = new Phone();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            phone.setId(jsonObject.getLong(idKey));
        }
        setDataToPhone(converterConfig, phone, jsonObject);

        return phone;
    }

    /**
     * Sets the json data into {@link Phone}
     *
     * @param converterConfig converter configuration
     * @param phone           which contains the phone data
     * @param jsonObject      which contains phone fields data
     */
    private void setDataToPhone(final ConverterConfig converterConfig, final Phone phone, final JsonObject jsonObject) {
        final Map<StoreSubType, FieldSubType> phoneSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONE);
        final Map<StoreSubType, FieldSubType> phonesSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES);

        phone.setOrgId(converterConfig.orgId());

        if (Objects.nonNull(phoneSubtypes) && !phoneSubtypes.isEmpty() &&
                Objects.nonNull(phonesSubtypes) && !phonesSubtypes.isEmpty()) {
            final String phoneTypeKey = phoneSubtypes.get(StoreSubType.PHONE_TYPE).idAsString();
            final String phoneValueKey = phoneSubtypes.get(StoreSubType.PHONE_NUMBER).idAsString();
            final String phoneCountryCodeKey = phoneSubtypes.get(StoreSubType.COUNTRY_CODE).idAsString();
            final String phonesTypeKey = phonesSubtypes.get(StoreSubType.PHONE_TYPE).idAsString();
            final String phonesValueKey = phonesSubtypes.get(StoreSubType.PHONE_NUMBER).idAsString();
            final String phonesCountryCodeKey = phonesSubtypes.get(StoreSubType.COUNTRY_CODE).idAsString();
            final Type type = jsonObject.containsKey(phoneTypeKey) ? Type.valueOf(jsonObject.getInt(phoneTypeKey))
                    : jsonObject.containsKey(phonesTypeKey) ? Type.valueOf(jsonObject.getInt(phonesTypeKey))
                    : converterConfig.isPartialData() ? phone.getType() : Type.valueOf(WORK.getId());
            final Country country;

            if (jsonObject.containsKey(phoneCountryCodeKey)) {
                country = Country.getCountryById(jsonObject.getInt(phoneCountryCodeKey));

            } else if (jsonObject.containsKey(phonesCountryCodeKey)) {
                final JsonObject phonesCountryJson = jsonObject.optJsonObject(phonesCountryCodeKey);

                if (Objects.isNull(phonesCountryJson)) {
                    country = Country.getCountryById(jsonObject.getInt(phonesCountryCodeKey));
                } else {
                    country = Country.getCountryById(phonesCountryJson.getInt(FieldKeys.VALUE.getName()));
                }
            } else {
                country = phone.getCountry();
            }
            final String phoneNumberValue = jsonObject.optString(jsonObject.containsKey(phoneValueKey) ?
                    phoneValueKey : phonesValueKey, phone.getNumber());

            phone.setType(type);
            phone.setCountry(country);
            phone.setNumber(phoneNumberValue);
        }
    }

    /**
     * Converts {@link JsonObject} into {@link Phone}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the phone data
     * @return the {@link Phone}
     */
    private Phone buildPhoneByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Phone phone = phoneManager.get(converterConfig.orgId(),
                jsonObject.getLong(FieldSubTypeKeys.ID.getName()));

        setDataToPhone(converterConfig, phone, jsonObject);

        return phone;
    }

    /**
     * Converts {@link Phone} into {@link JsonObject}
     *
     * @param phone A model that holds the attributes of phone
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Phone phone) {
        return convert(phone, fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONE));
    }

    /**
     * Converts {@link Phone} into {@link JsonObject}
     *
     * @param phone A model that holds the attributes of phone
     * @return {@link JsonObject}
     */
    public JsonObject convertPhone(final Phone phone) {
        return convert(phone, fieldTypeService.groupSubtypesByStoreSubType(StoreType.PHONES));
    }

    /**
     * Converts {@link Phone} into {@link JsonObject}
     *
     * @param phone A model that holds the attributes of phone
     * @return {@link JsonObject}
     */
    private JsonObject convert(final Phone phone, final Map<StoreSubType, FieldSubType> groupedSubtypes) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final Country country = phone.getCountry();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), phone.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), phone.orgId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.PHONE_TYPE).idAsString(), phone.getType().getId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.PHONE_NUMBER).idAsString(), phone.getNumber());

            if (Objects.nonNull(country)) {
                jsonObject.put(groupedSubtypes.get(StoreSubType.COUNTRY_CODE).idAsString(), country.getId());
            }
        }

        return jsonObject;
    }

    /**
     * Converts {@link Phone} into {@link JsonObject}
     *
     * @param phone           A model that holds the attributes of phone
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Phone phone) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(phone);
        } else {
            jsonObject.put(FieldSubTypeKeys.ID.getName(), phone.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), phone.orgId());
            jsonObject.put(StoreSubType.PHONE_TYPE.getAliasName(), phone.getType().getId());
            jsonObject.put(StoreSubType.PHONE_NUMBER.getAliasName(), phone.getNumber());
            final Country country = phone.getCountry();

            if (Objects.nonNull(country)) {
                jsonObject.put(StoreSubType.COUNTRY.getAliasName(), country.getId());
            }
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts a {@link Phone} into a {@link JsonObject}.
     * </p>
     *
     * @param phone           A model of type {@link Phone} that holds the attributes of a phone.
     * @param storeType       The type of store associated with the phone, used for grouping subtypes.
     * @param converterConfig The configuration settings used for conversion, including locale.
     * @return A {@link JsonObject} containing the phone's attributes structured as JSON.
     */
    public JsonObject convertModelToJson(final Phone phone, final StoreType storeType, final ConverterConfig converterConfig) {
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final Country country = phone.getCountry();
            final Phone.Type type = phone.getType();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), phone.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), phone.orgId());
            jsonObject.put(groupedSubtypes.get(StoreSubType.PHONE_TYPE).idAsString(), type.convertModelToOptionJson(type, converterConfig));
            jsonObject.put(groupedSubtypes.get(StoreSubType.PHONE_NUMBER).idAsString(), phone.getNumber());

            if (Objects.nonNull(country)) {
                jsonObject.put(groupedSubtypes.get(StoreSubType.COUNTRY_CODE).idAsString(), convert(country, converterConfig));
            }
        }

        return jsonObject;
    }

    /**
     * Converts the specified {@code Country} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param country the {@code Country} object to be converted
     * @param converterConfig the {@code ConverterConfig} containing configuration details
     * @return a {@code JsonObject} representing the converted country, with fields for value and name
     */
    private JsonObject convert(final Country country, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), country.getId());
        jsonObject.put(FieldKeys.NAME.getName(), i18nService.getResolver(converterConfig.locale()).get(country.getName()));

        return jsonObject;
    }

    /**
     * <p>
     * Converts a {@link Phone} into a {@link JsonObject}.
     * </p>
     *
     * @param phone           A model of type {@link Phone} that holds the attributes of a phone.
     * @param converterConfig The configuration settings used for conversion, including locale.
     * @return A {@link JsonObject} containing the phone's attributes structured as JSON.
     */
    @Override
    public JsonObject convertModelToJson(final Phone phone, final ConverterConfig converterConfig) {
        return convertModelToJson(phone, StoreType.PHONE, converterConfig);
    }

    /**
     * {@inheritDoc}
     *
     * @param phone           A model of type {@link Phone} that holds the attributes of a phone.
     * @param converterConfig The configuration settings used for conversion, including locale.
     * @return A {@link JsonObject} containing the phone's attributes structured as JSON.
     */
    @Override
    public JsonObject convertToRestJson(final Phone phone, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final Country country = phone.getCountry();
        final Phone.Type type = phone.getType();

        jsonObject.put(FieldSubTypeKeys.ID.getName(), phone.id());
        jsonObject.put(StoreSubType.PHONE_TYPE.getApiPropertyName(), type.convertModelToOptionJson(type, converterConfig));
        jsonObject.put(StoreSubType.PHONE_NUMBER.getApiPropertyName(), phone.getNumber());

        if (Objects.nonNull(country)) {
            jsonObject.put(StoreSubType.COUNTRY_CODE.getApiPropertyName(), convert(country, converterConfig));
        }

        return jsonObject;
    }
}

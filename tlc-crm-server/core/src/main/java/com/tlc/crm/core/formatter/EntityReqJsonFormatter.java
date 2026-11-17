package com.tlc.crm.core.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.OptionService;
import com.tlc.crm.entity.models.type.Option;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.service.FieldService;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.model.country.Country;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Map;
import java.util.Optional;

/**
 * Formatter that converts an incoming REST request JSON for a entity into the
 * internal request JSON structure used by the entity layer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
abstract class EntityReqJsonFormatter {

    private static final String EMPTY_SPACE = " ";

    protected final FieldService fieldService;
    protected final FieldTypeService fieldTypeService;
    protected final OptionService optionService;

    private final BaseCurrencyService baseCurrencyService;

    protected EntityReqJsonFormatter() {
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.optionService = OptionService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * Maps a persistent field from the internal entity JSON to the Zapier action JSON format.
     *
     * @param orgId        The ID of the organization, used for field-specific formatting or lookups.
     * @param fieldInfo    Metadata describing the field to be mapped.
     * @param internalName Refers the internal name of the field.
     * @param entityJson   The source JSON object containing internal entity data.
     * @param requestJson  The target JSON object to be populated.
     * @param i18nResolver Refers the {@code I18nResolver}.
     */
    protected final void mapPersistentFieldToJson(final long orgId, final FieldInfo fieldInfo, final String internalName,
                                                  final JsonObject entityJson, final JsonObject requestJson, final I18nResolver i18nResolver) {
        final StoreType storeType = fieldInfo.storeType();
        final String idAsString = fieldInfo.idAsString();

        switch (storeType) {
            case SINGLE_STRING, TEXT -> {
                if (requestJson.containsKey(internalName)) {
                    entityJson.put(idAsString, requestJson.getString(internalName));
                }
            }
            case SINGLE_NUMBER, OWNER, COMPANY, CONTACT -> {
                if (requestJson.containsKey(internalName)) {
                    entityJson.put(idAsString, requestJson.getLong(internalName));
                }
            }
            case DROPDOWN -> {
                if (requestJson.containsKey(internalName)) {
                    mapOptionField(orgId, requestJson.get(internalName), fieldInfo, entityJson, i18nResolver);
                }
            }
            case EMAIL -> {
                if (requestJson.containsKey(internalName)) {
                    final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString(), jsonObject.opt(StoreSubType.EMAIL_VALUE.getApiPropertyName()));
                    mapEmailTypeField(jsonObject.opt(StoreSubType.EMAIL_TYPE.getApiPropertyName()), subFieldJson, fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString());
                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case PHONE -> {
                if (requestJson.containsKey(internalName)) {
                    final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString(), jsonObject.opt(StoreSubType.PHONE_NUMBER.getApiPropertyName()));
                    mapPhoneTypeField(jsonObject.opt(StoreSubType.PHONE_TYPE.getApiPropertyName()), subFieldJson, fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString());
                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case ADDRESS -> {

                if (requestJson.containsKey(internalName)) {
                    final JsonObject addressJson = requestJson.getJsonObject(internalName);
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                    final Object country = addressJson.opt(StoreSubType.COUNTRY.getApiPropertyName());

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), addressJson.opt(StoreSubType.ADDRESS_LINE1.getApiPropertyName()));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE2).idAsString(), addressJson.opt(StoreSubType.ADDRESS_LINE2.getApiPropertyName()));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.CITY).idAsString(), addressJson.opt(StoreSubType.CITY.getApiPropertyName()));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.STATE).idAsString(), addressJson.opt(StoreSubType.STATE.getApiPropertyName()));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.ZIPCODE).idAsString(), addressJson.opt(StoreSubType.ZIPCODE.getApiPropertyName()));
                    mapCountryField(country, subFieldJson, fieldSubTypes.get(StoreSubType.COUNTRY).idAsString());

                    entityJson.put(fieldInfo.idAsString(), subFieldJson);
                }
            }
            case MULTI_SELECT -> {
                if (requestJson.containsKey(internalName)) {
                    final JsonArray array = Json.array();

                    requestJson.getJsonArray(internalName).forEach(value -> {
                        if (TypeChecker.isNumber(value)) {
                            array.put(NumberUtils.parseNumber(value).longValue());
                        } else {
                            getOption(orgId, value, fieldInfo, i18nResolver).ifPresent(option -> array.put(option.id()));
                        }
                    });
                    entityJson.put(fieldInfo.idAsString(), array);
                }
            }
            case MONETARY -> {
                if (requestJson.containsKey(internalName)) {
                    final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
                    final Object currencyValue = jsonObject.opt(StoreSubType.CURRENCY.getApiPropertyName());

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), jsonObject.opt(StoreSubType.VALUE.getApiPropertyName()));

                    if (TypeChecker.isNumber(currencyValue)) {
                        subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), currencyValue);
                    } else {
                        subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), resolveCurrency(orgId, (String) currencyValue, i18nResolver).getId());
                    }
                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case DATE, TIME -> {
                if (requestJson.containsKey(internalName)) {
                    entityJson.put(idAsString, TemporalServiceProvider.get().getISOOffsetTimeStamp(requestJson.getString(internalName)));
                }
            }
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> {

                if (requestJson.containsKey(internalName)) {
                    final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    // TODO - Support date formatters based on user locale - #8160
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.FROM).idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(jsonObject.getString(StoreSubType.FROM.getApiPropertyName())));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.TO).idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(jsonObject.getString(StoreSubType.TO.getApiPropertyName())));

                    entityJson.put(idAsString, subFieldJson);
                }
            }
        }
    }

    /**
     * Maps an option field from the incoming request JSON to the internal entity JSON structure.
     *
     * @param orgId      Represents the organization ID.
     * @param value      Refers the value of the field.
     * @param fieldInfo  Refers the {@code FieldInfo}.
     * @param entityJson Refers the entity json.
     */
    protected final void mapOptionField(final long orgId, final Object value, final FieldInfo fieldInfo,
                                        final JsonObject entityJson, final I18nResolver i18nResolver) {
        final String idAsString = fieldInfo.idAsString();

        if (TypeChecker.isNumber(value)) {
            entityJson.put(idAsString, NumberUtils.parseNumber(value).longValue());
        } else {
            getOption(orgId, value, fieldInfo, i18nResolver)
                    .ifPresent(option -> entityJson.put(idAsString, option.id()));
        }
    }

    /**
     * Resolves an option for the specified field from the given value.
     *
     * @param orgId        The organization identifier
     * @param value        The value used to resolve the option
     * @param fieldInfo    The metadata of the field whose option is to be resolved
     * @param i18nResolver The resolver used to match localized option names
     * @return An {@link Optional} containing the resolved {@link Option} if a match is found; otherwise, {@link Optional#empty()}
     */
    protected final Optional<Option> getOption(final long orgId, final Object value, final FieldInfo fieldInfo, final I18nResolver i18nResolver) {
        return optionService.get(orgId, fieldInfo).stream()
                .filter(optionName -> i18nResolver.get(optionName.getOption()).replace(EMPTY_SPACE, StringUtil.EMPTY_STRING)
                        .equalsIgnoreCase(((String) value).replace(EMPTY_SPACE, StringUtil.EMPTY_STRING))).findFirst();
    }

    /**
     * Resolves the currency from the given input value.
     * <p>
     * The resolution is performed in the following order:
     * <ol>
     *   <li>Matches the input against a currency code (e.g. "USD").</li>
     *   <li>If no match is found, matches the localized currency name using the provided {@link I18nResolver}.</li>
     *   <li>If still unresolved, returns the organization's default currency.</li>
     * </ol>
     *
     * @param orgId         The organization identifier used to retrieve the default currency
     * @param currencyValue The currency code or localized currency name
     * @param i18nResolver  The resolver used to translate localized currency names
     * @return The resolved {@link Currency}, or the organization's default currency if no match is found
     */
    protected final Currency resolveCurrency(final long orgId, final String currencyValue, final I18nResolver i18nResolver) {
        return Currency.getCurrencyByCode(currencyValue.toUpperCase())
                .or(() -> Currency.getCurrencies().stream().filter(currency -> i18nResolver.get(currency.getI18nKey()).equalsIgnoreCase(currencyValue)).findFirst())
                .orElseGet(() -> baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency());
    }

    /**
     * Maps a currency field value to its corresponding identifier and adds it to the JSON entity.
     *
     * @param value      The currency value to map.
     * @param entityJson The target JSON object.
     * @param key        The JSON property key.
     */
    protected final void mapCountryField(final Object value, final JsonObject entityJson, final String key) {
        if (TypeChecker.isNumber(value)) {
            entityJson.put(key, value);
        } else {
            Country.getCountryByName(((String) value).toUpperCase())
                    .ifPresent(country -> entityJson.put(key, country.getId()));
        }
    }

    /**
     * Maps an email type value to its corresponding identifier and adds it to the JSON entity.
     * If the provided value is invalid or unsupported, the default email type is used.
     *
     * @param value      The email type value to map.
     * @param entityJson The target JSON object.
     * @param key        The JSON property key.
     */
    protected final void mapEmailTypeField(final Object value, final JsonObject entityJson, final String key) {
        if (TypeChecker.isNumber(value)) {
            entityJson.put(key, value);
        } else {
            entityJson.put(key, Email.Type.get((String) value).getId());
        }
    }

    /**
     * Maps a phone type value to its corresponding identifier and adds it to the JSON entity.
     * If the provided value is invalid or unsupported, the default phone type is used.
     *
     * @param value      The phone type value to map.
     * @param entityJson The target JSON object.
     * @param key        The JSON property key.
     */
    protected final void mapPhoneTypeField(final Object value, final JsonObject entityJson, final String key) {
        if (TypeChecker.isNumber(value)) {
            entityJson.put(key, value);
        } else {
            entityJson.put(key, Phone.Type.get((String) value).getId());
        }
    }

    /**
     * Formats the incoming request JSON into the internal entity JSON structure expected by the CRM system. This method is abstract and should be implemented by subclasses to provide specific formatting logic based on the entity type and organizational requirements.
     *
     * @param orgId        Represents the organization ID for which the formatting is being done, allowing for organization-specific logic if necessary.
     * @param requestJson  Represents the incoming JSON object from the request that needs to be formatted into the internal entity JSON structure.
     * @param i18nResolver Represents the internationalization resolver that can be used to fetch localized strings or handle locale-specific formatting during the conversion process.
     * @return JsonObject that is formatted according to the internal entity structure, ready for processing by the CRM system.
     */
    protected abstract JsonObject format(final long orgId, final JsonObject requestJson, final I18nResolver i18nResolver);
}

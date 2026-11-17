package com.tlc.crm.entity.models.type.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.manager.field.type.MonetaryManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;
import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Monetary}
 *
 * @author Sundar
 * @version 1.0
 */
public class MonetaryConverter extends EntityConverter<Monetary> {

    private static final int DESIRED_SCALE = 4;
    private final MonetaryManager monetaryManager;
    protected final FieldTypeService fieldTypeService;
    private final BaseCurrencyService baseCurrencyService;

    private static class Instance {
        private static final MonetaryConverter INSTANCE = new MonetaryConverter();
    }

    protected MonetaryConverter() {
        this.monetaryManager = MonetaryManager.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
    }

    /**
     * Gets the {@link MonetaryConverter} instance
     *
     * @return {@link MonetaryConverter}
     */
    public static MonetaryConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Monetary}
     * <p>
     * import java.math.BigDecimal;
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the monetary data
     * @return the {@link Monetary}
     */
    @Override
    public Monetary convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildMonetaryByPartialData(converterConfig, jsonObject)
                : buildMonetary(converterConfig, jsonObject);
    }

    /**
     * Converts {@link JsonObject} into {@link Monetary}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the monetary data
     * @return the {@link Monetary}
     */
    private Monetary buildMonetary(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Monetary monetary = new Monetary();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            monetary.setId(jsonObject.getLong(idKey));
        }
        setDataToMonetary(converterConfig, monetary, jsonObject);

        return monetary;
    }

    /**
     * Sets the json data into {@link Monetary}
     *
     * @param converterConfig converter configuration
     * @param monetary        which contains the monetary data
     * @param jsonObject      which contains monetary fields data
     */
    public void setDataToMonetary(final ConverterConfig converterConfig, final Monetary monetary, final JsonObject jsonObject) {
        final Map<StoreSubType, FieldSubType> monetarySubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.MONETARY);
        final Map<StoreSubType, FieldSubType> dealValueSubTypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);
        final Long orgId = converterConfig.orgId();

        monetary.setOrgId(orgId);

        if (Objects.nonNull(monetarySubTypes) && !monetarySubTypes.isEmpty()
                && Objects.nonNull(dealValueSubTypes) && !dealValueSubTypes.isEmpty()) {
            final String monetaryCurrencyKey = monetarySubTypes.get(StoreSubType.CURRENCY).idAsString();
            final String monetaryValueKey = monetarySubTypes.get(StoreSubType.VALUE).idAsString();
            final String dealValueCurrencyKey = dealValueSubTypes.get(StoreSubType.CURRENCY).idAsString();
            final String dealValueKey = dealValueSubTypes.get(StoreSubType.VALUE).idAsString();
            final short currencyId = jsonObject.getShort(jsonObject.containsKey(monetaryCurrencyKey) ? monetaryCurrencyKey : dealValueCurrencyKey);
            final Currency currency = Currency.getCurrencyById(currencyId);

            monetary.setCurrency(currency);

            if (Objects.nonNull(monetaryValueKey) && Objects.nonNull(dealValueKey)) {
                final Object value;

                if (jsonObject.containsKey(monetaryValueKey)) {
                    value = jsonObject.isKeyPresentWithNullValue(monetaryValueKey) ? BigDecimal.ZERO : jsonObject.get(monetaryValueKey);
                } else if (jsonObject.containsKey(dealValueKey)) {
                    value = jsonObject.isKeyPresentWithNullValue(dealValueKey) ? BigDecimal.ZERO : jsonObject.get(dealValueKey);
                } else {
                    value = BigDecimal.ZERO;
                }

                if (value.toString().isBlank()) {
                    monetary.setValue(BigDecimal.ZERO);
                } else {
                    final RoundingMode roundingMode = RoundingMode.HALF_UP;
                    BigDecimal monetaryValue = BigDecimal.valueOf(NumberUtils.parseNumber(value).doubleValue());

                    monetaryValue = monetaryValue.setScale(DESIRED_SCALE, roundingMode);
                    monetary.setValue(monetaryValue);
                }
            }
        }
    }

    /**
     * Converts {@link JsonObject} into {@link Monetary}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the monetary data
     * @return the {@link Monetary}
     */
    private Monetary buildMonetaryByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final String idKey = FieldSubTypeKeys.ID.getName();
        final Monetary monetary;

        if (jsonObject.containsKey(idKey)) {
            monetary = monetaryManager.get(converterConfig.orgId(), jsonObject.getLong(idKey));
        } else {
            monetary = new Monetary();
        }
        setDataToMonetary(converterConfig, monetary, jsonObject);

        return monetary;
    }

    /**
     * Converts {@link Monetary} into {@link JsonObject}
     *
     * @param monetary A model that holds the attributes of monetary
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Monetary monetary) {
        final JsonObject jsonObject = Json.object();
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.MONETARY);

        loadMonetaryJson(monetary, jsonObject, groupedSubtypes);

        return jsonObject;
    }

    /**
     * Loads the monetary data into {@link JsonObject}.
     *
     * @param monetary        Refers the monetary details.
     * @param jsonObject      Refers the json which holds the monetary details.
     * @param groupedSubtypes Refers the {@link FieldSubType}.
     */
    protected void loadMonetaryJson(final Monetary monetary, final JsonObject jsonObject,
                                    final Map<StoreSubType, FieldSubType> groupedSubtypes) {
        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final BigDecimal value = monetary.getValue();
            final Currency currency = monetary.getCurrency();
            final Long orgId = monetary.orgId();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), monetary.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), orgId);
            jsonObject.put(groupedSubtypes.get(StoreSubType.VALUE).idAsString(), Objects.isNull(value) ? 0.0000
                    : baseCurrencyService.applyPrecision(orgId, value));

            if (Objects.nonNull(currency)) {
                jsonObject.put(groupedSubtypes.get(StoreSubType.CURRENCY).idAsString(), currency.getId());
                jsonObject.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
            }
        }
    }

    /**
     * Converts {@link Monetary} into {@link JsonObject}
     *
     * @param monetary        A model that holds the attributes of monetary
     * @param converterConfig Converter configuration settings
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final Monetary monetary) {
        final JsonObject jsonObject = Json.object();

        if (converterConfig.isIdRequiredOnKey()) {
            return convert(monetary);
        } else {
            final BigDecimal value = monetary.getValue();
            final Currency currency = monetary.getCurrency();
            final Long orgId = monetary.orgId();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), monetary.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), orgId);
            jsonObject.put(StoreSubType.VALUE.getAliasName(), Objects.isNull(value) ? 0.0000
                    : baseCurrencyService.applyPrecision(orgId, value));

            if (Objects.nonNull(currency)) {
                jsonObject.put(StoreSubType.CURRENCY.getAliasName(), currency.getId());
                jsonObject.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
            }
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param monetary Represents the entity model
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final Monetary monetary) {
        final JsonObject jsonObject = Json.object();
        final BigDecimal value = monetary.getValue();
        final Currency currency = monetary.getCurrency();

        jsonObject.put(FieldSubTypeKeys.ID.getName(), monetary.id());
        jsonObject.put(StoreSubType.VALUE.getAliasName(), Objects.isNull(value) ? 0.0000
                : baseCurrencyService.applyPrecision(monetary.orgId(), value));

        if (Objects.nonNull(currency)) {
            jsonObject.put(StoreSubType.CURRENCY.getAliasName(), currency.getId());
            jsonObject.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts the monetary value with corresponding default base currency symbol into {@link JsonObject}.
     * </p>
     *
     * @return {@link JsonObject} of monetary value
     */
    public JsonObject convertDefaultMonetaryToJson(final long orgId, final BigDecimal value) {
        final JsonObject monetaryJson = Json.object();

        monetaryJson.put(FieldSubTypeKeys.VALUE.getName(), baseCurrencyService.applyPrecision(orgId, value));
        monetaryJson.put(FieldSubTypeKeys.SYMBOL.getName(), baseCurrencyService.getDefaultCurrency(orgId).getSymbol());

        return monetaryJson;
    }

    /**
     * <p>
     * Converts the monetary value with corresponding symbol into {@link JsonObject}.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param monetary Represents the {@link Monetary}
     * @return {@link JsonObject} of monetary
     */
    public JsonObject convertMonetaryToJson(final long orgId, final Monetary monetary) {
        final JsonObject monetaryJson = Json.object();
        final Currency currency = monetary.getCurrency();

        monetaryJson.put(FieldSubTypeKeys.VALUE.getName(), baseCurrencyService.applyPrecision(orgId, monetary.getValue()));

        if (Objects.nonNull(currency)) {
            monetaryJson.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
        }

        return monetaryJson;
    }

    /**
     * <p>
     * Converts the monetary details of type {@link Monetary} into a {@link JsonObject}.
     * </p>
     *
     * @param monetary        The {@link Monetary} model that holds the monetary details to convert.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return A {@link JsonObject} containing the monetary data, structured according to the defined schema.
     */
    @Override
    public JsonObject convertModelToJson(final Monetary monetary, final ConverterConfig converterConfig) {
        return convertModelToJson(monetary, fieldTypeService.groupSubtypesByStoreSubType(StoreType.MONETARY), converterConfig);
    }

    /**
     * <p>
     * Converts the monetary details of type {@link Monetary} into a {@link JsonObject}
     * using the specified grouped subtypes.
     * </p>
     *
     * @param monetary        The {@link Monetary} model that holds the monetary details to convert.
     * @param groupedSubtypes A map of {@link StoreSubType} to {@link FieldSubType} that defines
     *                        the structure for JSON conversion.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return A {@link JsonObject} containing the monetary data, structured according to the defined schema.
     */
    protected JsonObject convertModelToJson(final Monetary monetary, final Map<StoreSubType, FieldSubType> groupedSubtypes,
                                            final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();

        if (Objects.nonNull(groupedSubtypes) && !groupedSubtypes.isEmpty()) {
            final BigDecimal value = monetary.getValue();
            final Currency currency = monetary.getCurrency();
            final Long orgId = monetary.orgId();

            jsonObject.put(FieldSubTypeKeys.ID.getName(), monetary.id());
            jsonObject.put(FieldSubTypeKeys.ORG_ID.getName(), orgId);
            jsonObject.put(groupedSubtypes.get(StoreSubType.VALUE).idAsString(), Objects.isNull(value) ? 0.0000
                    : baseCurrencyService.applyPrecision(orgId, value));

            if (Objects.nonNull(currency)) {
                jsonObject.put(groupedSubtypes.get(StoreSubType.CURRENCY).idAsString(), convert(currency));
                jsonObject.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
            }
        }

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @param monetary        The {@link Monetary} model that holds the monetary details to convert.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return A {@link JsonObject} containing the monetary data, structured according to the defined schema.
     */
    @Override
    public JsonObject convertToRestJson(final Monetary monetary, final ConverterConfig converterConfig) {
        final JsonObject jsonObject = Json.object();
        final BigDecimal value = monetary.getValue();
        final Currency currency = monetary.getCurrency();
        final Long orgId = monetary.orgId();

        jsonObject.put(FieldSubTypeKeys.ID.getName(), monetary.id());
        jsonObject.put(StoreSubType.VALUE.getApiPropertyName(), Objects.isNull(value) ? 0.0000 : baseCurrencyService.applyPrecision(orgId, value));

        if (Objects.nonNull(currency)) {
            jsonObject.put(StoreSubType.CURRENCY.getApiPropertyName(), convert(currency));
            jsonObject.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
        }

        return jsonObject;
    }

    /**
     * Converts the specified {@code Currency} object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param currency        the {@code Currency} object to be converted
     * @return a {@code JsonObject} representing the converted currency, with fields for value and name
     */
    public JsonObject convert(final Currency currency) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(FieldKeys.VALUE.getName(), currency.getId());
        jsonObject.put(FieldKeys.NAME.getName(), currency.getCode());

        return jsonObject;
    }
}
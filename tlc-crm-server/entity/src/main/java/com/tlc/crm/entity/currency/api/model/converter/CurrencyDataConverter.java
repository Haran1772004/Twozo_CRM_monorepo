package com.tlc.crm.entity.currency.api.model.converter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.currency.action.config.CurrencyConverterConfig;
import com.tlc.crm.entity.currency.api.model.CurrencyData;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;

/**
 * <p>
 * Converts the services of {@link CurrencyData} to the relevant data.
 * </p>
 *
 * @author KowsalyaSP
 * @see EntityConverter
 */
public class CurrencyDataConverter extends EntityConverter<CurrencyData> {

    private final CurrencyUnitConverter currencyUnitConverter;

    private static class Instance {
        private static final CurrencyDataConverter INSTANCE = new CurrencyDataConverter();
    }

    private CurrencyDataConverter() {
        this.currencyUnitConverter = CurrencyUnitConverter.getInstance();
    }

    /**
     * <p>
     * Gets the instance of {@link CurrencyDataConverter}.
     * </p>
     *
     * @return {@link CurrencyDataConverter}
     */
    public static CurrencyDataConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Represents the {@link JsonObject}
     * @return {@link CurrencyData}
     */
    @Override
    public CurrencyData convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        throw ErrorCode.get(EntityErrorCodes.NOT_SUPPORTED, "i18n.currency.unit.unsupported.action");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param currencyData    Represents the {@link CurrencyData}
     * @return currency data {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final CurrencyData currencyData) {
        final JsonObject currencyJson = currencyUnitConverter.convert((CurrencyConverterConfig) converterConfig, currencyData.currency());

        currencyJson.put(FieldSubTypeKeys.IS_ACTIVE.getName(), currencyData.isActive());

        return currencyJson;
    }
}

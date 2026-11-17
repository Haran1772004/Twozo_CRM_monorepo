package com.tlc.exchange.rate.internal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.exchange.rate.internal.status.ExchangeRateErrorCodes;
import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import java.io.Serializable;

import java.math.BigDecimal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Provides the functionality of storage and retrieval of exchange rates between diverse currencies,
 * ensuring currency exchange information that is current and up-to-date.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class ExchangeRateContainer implements ReadableExchangeRateContainer, Serializable {

    private Long id;
    private Currency baseCurrency;
    private Map<Currency, BigDecimal> exchangeRates;
    private JsonObject exchangeRateJson;
    private String exchangeRateJsonAsString;
    private boolean isExchangeRateJsonAvailable;

    private ExchangeRateContainer(final Map<Currency, BigDecimal> exchangeRates) {
        this.exchangeRates = exchangeRates;
    }

    public ExchangeRateContainer() {
        this(new ConcurrentHashMap<>());
    }

    public ExchangeRateContainer(final String jsonAsString) {
        this();
        loadExchangeRates(jsonAsString, exchangeRates);
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(final Currency baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Map<Currency, BigDecimal> getExchangeRates() {
        return exchangeRates;
    }

    public void setExchangeRates(final Map<Currency, BigDecimal> exchangeRates) {
        this.exchangeRates = exchangeRates;

        updateExchangeRateJsonAvailability();
    }

    public JsonObject getExchangeRateJson() {
        this.exchangeRateJson = isExchangeRateJsonAvailable ? exchangeRateJson : convertExchangeRateToJson();

        return exchangeRateJson;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the exchange rate json as string
     */
    @Override
    public String toString() {
        this.exchangeRateJsonAsString = isExchangeRateJsonAvailable ? exchangeRateJsonAsString : getExchangeRateJson().toString();

        return exchangeRateJsonAsString;
    }

    /**
     * <p>
     * Adds the exchange rate for the respective target {@link Currency} into the rate container.
     * </p>
     *
     * @param targetCurrency Represents the target {@link Currency}
     * @param exchangeRate   Represents the exchange rate for the respective {@link Currency}
     */
    public void addExchangeRate(final Currency targetCurrency, final BigDecimal exchangeRate) {
        exchangeRates.put(targetCurrency, exchangeRate);

        updateExchangeRateJsonAvailability();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    @Override
    public BigDecimal getExchangeRate(final Currency targetCurrency) {

        if (exchangeRates.containsKey(targetCurrency)) {
            return exchangeRates.get(targetCurrency);
        }

        throw ErrorCode.get(ExchangeRateErrorCodes.TARGET_CURRENCY_ACCESS_DENIED, "i18n.currency.target.currency.access.denied");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return If the container is empty, it returns true; otherwise, it returns false
     */
    @Override
    public boolean isEmpty() {
        return exchangeRates.isEmpty();
    }

    /**
     * <p>
     * Converts the exchange rate to a {@link JsonObject} format.
     * </p>
     *
     * @return {@link JsonObject}
     */
    private JsonObject convertExchangeRateToJson() {
        final JsonObject exchangeRateJson = Json.object();

        for (final Map.Entry<Currency, BigDecimal> rateEntry : exchangeRates.entrySet()) {
            exchangeRateJson.put(rateEntry.getKey().getCode(), rateEntry.getValue());
        }

        this.isExchangeRateJsonAvailable = true;

        return exchangeRateJson;
    }

    /**
     * <p>
     * Loads the exchange rates from the given string representation and
     * returns them as a map, associating each currency with its corresponding rate.
     * </p>
     *
     * @param jsonAsString Represents the exchange rates in string format
     * @param rates        Represents the map containing the extracted exchange rates
     */
    private void loadExchangeRates(final String jsonAsString, final Map<Currency, BigDecimal> rates) {
        final JsonObject exchangeRateJson = Json.object(jsonAsString);

        for (final Map.Entry<String, Object> rate : exchangeRateJson.toMap().entrySet()) {
            rates.put(Currency.valueOf(rate.getKey()), BigDecimal.valueOf((Double) rate.getValue()));
        }
    }

    /**
     * <p>
     * Updates the flag indicating its availability to false.
     * </p>
     */
    private void updateExchangeRateJsonAvailability() {

        if (isExchangeRateJsonAvailable) {
            this.exchangeRateJson = null;
            this.isExchangeRateJsonAvailable = false;
        }
    }
}


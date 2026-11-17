package com.tlc.exchange.rate.internal.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.l10n.model.currency.Currency;
import com.tlc.commons.json.JsonObject;

import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;
import com.tlc.exchange.rate.internal.status.ExchangeRateErrorCodes;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import java.net.URL;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * Handles the fixer exchange-rate related tasks, offering functionalities for currency exchange and formatting.
 * It relies on external data sources for live exchange rates and is designed for financial calculations,
 * providing methods for historical rate retrieval and other related operations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractExchangeRateService
 */
public final class FixerExchangeRateService extends AbstractExchangeRateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixerExchangeRateService.class);

    private static final String API_URL = "api.url";
    private static final String API_KEY = "api.key";
    private static final String DEV_MODE = "dev.mode";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String SUCCESS_FLAG_KEY = "true";
    private static final String FAILURE_FLAG_KEY = "false";
    private static final String ACCESS_KEY = "access_key";
    private static final String BASE = "base";
    private static final String SYMBOLS = "symbols";
    private static final String RATES = "rates";
    private static final String SUCCESS = "success";
    private static final String ERROR = "error";
    private static final String CODE = "code";

    private static final Collection<String> DEV_CURRENCY_CODES = Set.of("USD", "INR", "EUR", "KWD", "BHD", "GBP", "KYD", "OMR", "CHF", "AED");

    private String apiKey;
    private String apiUrl;
    private boolean isDevMode;
    private Collection<String> currencyCodes;
    private Collection<Currency> supportedCurrencies;

    public FixerExchangeRateService() {
        super();
        this.supportedCurrencies = new HashSet<>();
        this.currencyCodes = new HashSet<>();
    }

    /**
     * <p>
     * Sets the properties for currency service.
     * </p>
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        this.apiKey = configurations.getConfig(API_KEY);
        this.apiUrl = configurations.getConfig(API_URL);
        this.isDevMode = Boolean.parseBoolean(configurations.optConfig(DEV_MODE, FAILURE_FLAG_KEY));
    }

    @Override
    public void stop() {
        this.apiKey = null;
        this.apiUrl = null;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency Represents the base currency
     * @return the exchange rates with respective currency
     */
    @Override
    public ExchangeRateContainer getOnlineExchangeRateContainer(final Currency baseCurrency) {
        final Map<String, String> queryParams = new HashMap<>();

        queryParams.put(BASE, baseCurrency.getCode());
        queryParams.put(ACCESS_KEY, apiKey);

        return getExchangeRateContainer(queryParams);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency   Represents the base currency
     * @param targetCurrency Represents the target currency
     * @return the exchange rate
     */
    @Override
    public BigDecimal getOnlineExchangeRates(final Currency baseCurrency, final Currency targetCurrency) {
        return getOnlineExchangeRateContainer(baseCurrency, Set.of(targetCurrency)).getExchangeRate(targetCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency     Represents the base currency
     * @param targetCurrencies Represents the collection of target currencies
     * @return the {@link ExchangeRateContainer} for the respective currencies
     */
    @Override
    public ExchangeRateContainer getOnlineExchangeRateContainer(final Currency baseCurrency, final Collection<Currency> targetCurrencies) {
        final Map<String, String> queryParams = new HashMap<>();

        queryParams.put(SYMBOLS, String.join(",", targetCurrencies.stream().map(Currency::getCode).toList()));
        queryParams.put(BASE, baseCurrency.getCode());
        queryParams.put(ACCESS_KEY, apiKey);

        return getExchangeRateContainer(queryParams);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return the supported {@link Collection} of {@link Currency}
     */
    @Override
    public Collection<Currency> getSupportedCurrencies() {
        this.supportedCurrencies = this.supportedCurrencies.isEmpty() ? getCurrencies() : supportedCurrencies;

        return supportedCurrencies;
    }

    /**
     * <p>
     * Retrieves the {@link Collection} of supported {@link Currency} by comparing the currencies
     * offered by external providers with those supported by our service.
     * If a currency matches the supported currencies, it is considered supported and returned.
     * </p>
     *
     * @return A collection of supported currencies provided by our service.
     */
    private Collection<Currency> getCurrencies() {
        final Map<String, String> queryParams = new HashMap<>();
        final HttpRequest httpRequest;

        queryParams.put(ACCESS_KEY, apiKey);

        try {
            httpRequest = new HttpRequest(new URL(appendQueryParams(apiUrl.concat(SYMBOLS), queryParams)), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap());
        } catch (Exception e) {
            throw ErrorCode.get(ExchangeRateErrorCodes.INVALID_URL);
        }

        final JsonObject response = HttpUtil.makeRequest(httpRequest);

        if (Objects.equals(SUCCESS_FLAG_KEY, response.getString(SUCCESS))) {
            final Map<String, String> symbols = (Map<String, String>) response.get(SYMBOLS);
            final Collection<String> currencyCodes = getCurrencyCodes();
            final Collection<Currency> supportedCurrencies = new ArrayList<>();

            for (final Map.Entry<String, String> symbol : symbols.entrySet()) {
                final String code = symbol.getKey();

                if (currencyCodes.contains(code)) {
                    supportedCurrencies.add(Currency.valueOf(code));
                }
            }

            return supportedCurrencies;
        } else {
            final JsonObject jsonObject = response.getJsonObject(ERROR);

            throw ErrorCode.get(ExchangeRateErrorCodes.getErrorCode(jsonObject.getInt(CODE)));
        }
    }

    /**
     * <p>
     * Fetches the current exchange rates for the specified currencies from an external data source
     * and the rates are up-to-date and reflect the most recent currency exchange rate values.
     * </p>
     *
     * @param queryParams Represents the query params
     * @return the exchange rates
     */
    private ExchangeRateContainer getExchangeRateContainer(final Map<String, String> queryParams) {
        final String date = getScheduledDate(ZonedDateTime.now(ZoneOffset.UTC));
        final HttpRequest httpRequest;

        try {
            httpRequest = new HttpRequest(new URL(appendQueryParams(apiUrl.concat(date), queryParams)), Collections.emptyMap(), HttpMethod.GET, Collections.emptyMap());
        } catch (Exception e) {
            throw ErrorCode.get(ExchangeRateErrorCodes.INVALID_URL);
        }

        final JsonObject response = HttpUtil.makeRequest(httpRequest);

        if (Objects.equals(SUCCESS_FLAG_KEY, response.getString(SUCCESS))) {
            final String baseCurrency = (String) response.get(BASE);
            final Map<String, Object> rates = (Map<String, Object>) response.get(RATES);
            final Collection<String> currencyCodes = getCurrencyCodes();
            final ExchangeRateContainer exchangeRateContainer = new ExchangeRateContainer();

            exchangeRateContainer.setBaseCurrency(Currency.valueOf(baseCurrency));

            for (final Map.Entry<String, Object> rate : rates.entrySet()) {
                final String code = rate.getKey();

                if (currencyCodes.contains(code)) {
                    final Number value = (Number) rate.getValue();

                    exchangeRateContainer.addExchangeRate(Currency.valueOf(code), BigDecimal.valueOf(value.doubleValue()));
                }
            }

            return exchangeRateContainer;
        } else {
            final JsonObject jsonObject = response.getJsonObject(ERROR);

            throw ErrorCode.get(ExchangeRateErrorCodes.getErrorCode(jsonObject.getInt(CODE)));
        }
    }

    /**
     * <p>
     * Constructs a new URL by appending the specified query parameters to the provided base URL.
     * If the base URL already contains query parameters, the new parameters are added to the existing ones.
     * </p>
     *
     * @param baseUrl     Represents the base url of the link
     * @param queryParams Represents the query params
     * @return the url with query params
     */
    private String appendQueryParams(final String baseUrl, final Map<String, String> queryParams) {
        final StringBuilder urlStringBuilder = new StringBuilder(baseUrl);

        if (Objects.nonNull(queryParams) && !queryParams.isEmpty()) {
            urlStringBuilder.append("?");

            int count = 0;

            for (final Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlStringBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)).append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));

                if (++count < queryParams.size()) {
                    urlStringBuilder.append("&");
                }
            }
        }

        return urlStringBuilder.toString();
    }

    /**
     * <p>
     * Gets the scheduled date for to update the exchange rate daily, precisely at 00:01 AM (UTC), considering the provided current date and time.
     * Note: If the current time is before 00:01 AM, the method considers it as the day before, and if it's after 00:01 AM, it considers the previous day.
     * </p>
     *
     * @param currentDateTime Denotes the current date and time in Coordinated Universal Time (UTC).
     * @return the scheduled date
     */
    private String getScheduledDate(final ZonedDateTime currentDateTime) {
        final ZonedDateTime startOfCurrentDay = currentDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
        final ZonedDateTime executionTime = currentDateTime.withHour(0).withMinute(1).withSecond(0);
        final ZonedDateTime time;

        if (currentDateTime.isAfter(executionTime)) {
            time = executionTime.minusDays(1);
        } else if (currentDateTime.isEqual(startOfCurrentDay)) {
            time = executionTime.minusDays(2);
        } else {
            time = executionTime;
        }

        return time.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    /**
     * <p>
     * Provides flexibility in accessing currency codes based on the development type.
     * When the environment type is set to {@code SUCCESS_FLAG_KEY}, it retrieves all available
     * currency codes, typically indicating a remote environment. Alternatively, when the
     * development type is set to {@code false}, it accesses only specific currency codes
     * defined locally, typically indicating a local environment.
     * </p>
     *
     * @return {@link Collection} of currency codes
     */
    private Collection<String> getCurrencyCodes() {

        if (this.currencyCodes.isEmpty()) {
            this.currencyCodes = isDevMode ? DEV_CURRENCY_CODES : Currency.getCodes();
        }

        return currencyCodes;
    }
}

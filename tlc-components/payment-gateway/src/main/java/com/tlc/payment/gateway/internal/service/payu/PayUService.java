package com.tlc.payment.gateway.internal.service.payu;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;

import com.tlc.l10n.model.country.Country;

import com.tlc.payment.gateway.internal.fields.PaymentFields;
import com.tlc.payment.gateway.internal.status.PaymentGatewayErrorCodes;
import com.tlc.payment.gateway.manager.PaymentManager;
import com.tlc.payment.gateway.models.Address;
import com.tlc.payment.gateway.models.BillingAmount;
import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PayerLocalDetails;
import com.tlc.payment.gateway.models.Payment;
import com.tlc.payment.gateway.models.PaymentMethodInfo;
import com.tlc.payment.gateway.models.PaymentResponse;
import com.tlc.payment.gateway.models.PaymentMetaData;
import com.tlc.payment.gateway.models.PaymentMethod;
import com.tlc.payment.gateway.models.PaymentStatus;
import com.tlc.payment.gateway.service.PaymentService;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebExchange;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Handles PayU gateway payment operations: creating/destroying payers, managing payment methods,
 * confirming payments, and mapping gateway responses to domain objects.
 * <p>
 * Implements {@link PaymentService} and orchestrates API calls to PayU REST endpoints
 * and local data management through {@link PaymentManager}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PayUService implements PaymentService {

    private static final String REDIRECTION_URL_FORMAT = "%s?transactionId=%s&url=%s";

    private static String paymentLinkUrl;
    private static String tokenGenerationUrl;
    private static String redirectionUrl;
    private static String clientRedirectionUrl;
    private static String merchantId;
    private static String clientId;
    private static String clientSecret;

    @Override
    public void start(final Configurations configurations) {
        final Configurations urlConfigs = configurations.getNestedConfigs(PaymentFields.URL);

        paymentLinkUrl = urlConfigs.getConfig(PaymentFields.PAYMENT_LINK);
        tokenGenerationUrl = urlConfigs.getConfig(PaymentFields.TOKEN_GENERATION);
        redirectionUrl = urlConfigs.getConfig(PaymentFields.REDIRECTION);
        clientRedirectionUrl = urlConfigs.getConfig(PaymentFields.CLIENT_REDIRECTION);
        merchantId = configurations.getConfig(PaymentFields.MERCHANT_DOT_ID);
        clientId = configurations.getConfig(PaymentFields.CLIENT_DOT_ID);
        clientSecret = configurations.getConfig(PaymentFields.CLIENT_DOT_SECRET);
    }

    @Override
    public void stop() {
    }

    /**
     * {@inheritDoc}
     *
     * @return Collection of payment methods supported by the gateway.
     */
    @Override
    public Collection<PaymentMethod> getPaymentMethods() {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param payment The payment object containing payment details to be confirmed.
     * @return A response object with response details of the payment.
     */
    @Override
    public PaymentResponse makePayment(final Payment payment) {
        final Payer payer = payment.getPayer();
        final BillingAmount billingAmount = payment.getPrice();
        final String redirectionUrl = buildRedirectionUrl(payment.getLocalTransactionId());

        final JsonObject data = Json.object()
                .put(PaymentFields.IS_AMOUNT_FILLED_BY_CUSTOMER, false)
                .put(PaymentFields.IS_PARTIAL_PAYMENT_ALLOWED, false)
                .put(PaymentFields.VIA_EMAIL, false)
                .put(PaymentFields.VIA_SMS, false)
                .put(PaymentFields.MAX_PAYMENTS_ALLOWED, 1)
                .put(PaymentFields.UDF, Json.object()
                        .put(PaymentFields.UDF1, payment.getLocalTransactionId())
                        .put(PaymentFields.UDF2, HttpUtil.getDomain()))
                .put(PaymentFields.TRANSACTION_ID, payment.getTransactionId())
                .put(PaymentFields.SOURCE, PaymentFields.API)
                .put(PaymentFields.DESCRIPTION, payment.getOrderItem())
                .put(PaymentFields.CURRENCY, billingAmount.getCurrency().getCode())
                .put(PaymentFields.SUB_AMOUNT, billingAmount.getAmount())
                .put(PaymentFields.TAX, billingAmount.getTax())
                .put(PaymentFields.DISCOUNT, billingAmount.getDiscount())
                .put(PaymentFields.ADDRESS, buildAddressJson(payer.getAddress()))
                .put(PaymentFields.CUSTOMER, buildCustomerJson(payer))
                .put(PaymentFields.SUCCESS_URL, redirectionUrl)
                .put(PaymentFields.FAILURE_URL, redirectionUrl);

        final URL url = buildURL(paymentLinkUrl, PaymentFields.PAYMENT_LINKS);
        final HttpRequest httpRequest = new HttpRequest(url, Map.of(PaymentFields.REQUEST_BODY, data.toString()),
                HttpMethod.POST, getDefaultHeaders(PaymentFields.CREATE_PAYMENT_LINKS));
        final JsonObject response = HttpUtil.makeRequest(httpRequest);

        return convertJsonIntoPaymentResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param payment The payment object containing payment details to be confirmed.
     * @return A response object with confirmation details of the payment.
     */
    @Override
    public PaymentResponse confirmPayment(final Payment payment) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param payer The payer object containing details of the payer to be created.
     */
    @Override
    public void createPayer(final Payer payer) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * Deletes an existing payer from the system.
     *
     * @param payer The payer object containing details of the payer to be deleted.
     */
    @Override
    public void deletePayer(final Payer payer) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param payerLocalDetails Holds the details of local payer.
     * @return Collection of stored payment type info of the specified payer.
     */
    @Override
    public Collection<PaymentMethodInfo> getUsersPaymentMethods(final PayerLocalDetails payerLocalDetails) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param paymentMetaData Holds the details of payment.
     * @return The stored payment type info associated with the given token.
     */
    @Override
    public PaymentMethodInfo getUsersPaymentMethod(final PaymentMetaData paymentMetaData) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * Constructs the final, fully qualified redirection URL used after a payment transaction.
     *
     * @return the fully qualified and formatted redirection URL string.
     */
    private String buildRedirectionUrl(final long transactionId) {
        final String redirectionUrl = HttpUtil.buildAbsoluteUrl(PayUService.redirectionUrl);
        final String clientRedirectionUrl = HttpUtil.buildAbsoluteUrl(PayUService.clientRedirectionUrl);

        return String.format(REDIRECTION_URL_FORMAT, redirectionUrl, transactionId, clientRedirectionUrl);
    }

    /**
     * Utility to build URLs for PayU API requests.
     *
     * @param pathElements parts of the API path
     * @return constructed URL object
     * @throws ErrorCode if malformed URL
     */
    private URL buildURL(final String... pathElements) {
        final StringJoiner stringJoiner = new StringJoiner(StringUtil.Delimiter.FORWARD_SLASH.getDelimiter());

        for (final String pathElement : pathElements) {
            stringJoiner.add(pathElement);
        }

        try {
            return new URL(stringJoiner.toString());
        } catch (MalformedURLException exception) {
            throw ErrorCode.get(PaymentGatewayErrorCodes.INVALID_URL, exception);
        }
    }

    /**
     * Converts a PayU JSON response into a domain {@link PaymentResponse} object.
     *
     * @param jsonObject PayU response
     * @return mapped {@link PaymentResponse}
     */
    private PaymentResponse convertJsonIntoPaymentResponse(final JsonObject jsonObject) {
        final JsonObject resultJson = jsonObject.getJsonObject(PaymentFields.RESULT);
        final PaymentResponse.Builder paymentResponseBuilder = new PaymentResponse.Builder();

        paymentResponseBuilder.setStatus(PaymentStatus.PAYMENT_PENDING);
        paymentResponseBuilder.setCheckoutURL(resultJson.getString(PaymentFields.PAYMENT_LINK));
        return paymentResponseBuilder.build();
    }

    /**
     * Generates an OAuth access token using client credentials and a specified scope.
     * Sends a POST request to the OAuth token endpoint with client ID, client secret,
     * grant type, and scope parameters encoded as URL form data.
     *
     * @param scope the OAuth scope string for which the token is requested
     * @return the OAuth access token as a String
     */
    private String getAccessToken(final String scope) {
        final URL url = buildURL(tokenGenerationUrl, PaymentFields.OAUTH, PaymentFields.TOKEN);
        final Map<String, String> headers = Map.of(WebExchange.Header.CONTENT_TYPE.getName(), WebExchange.ContentType.URL_ENCODED_CONTENT.getName());
        final Map<String, String> params = Map.of(PaymentFields.CLIENT_ID, clientId,
                PaymentFields.CLIENT_SECRET, clientSecret,
                PaymentFields.GRANT_TYPE, PaymentFields.CLIENT_CREDENTIALS,
                PaymentFields.SCOPE, scope);
        final JsonObject jsonObject = HttpUtil.makeRequest(new HttpRequest(url, params, HttpMethod.POST, headers));

        return jsonObject.getString(PaymentFields.ACCESS_TOKEN);
    }

    /**
     * Constructs a JSON object representing an Address, based on provided Address model.
     * Maps address properties such as line1, line2, city, state, country, and zip code
     * into JSON fields expected by PayU API.
     *
     * @param address the Address model object containing address data
     * @return a JsonObject properly formatted for PayU API address input
     */
    private JsonObject buildAddressJson(final Address address) {
        final JsonObject addressJson = Json.object();
        final Country country = address.getCountry();

        addressJson.put(PaymentFields.LINE_1, address.getAddressLine1());
        addressJson.put(PaymentFields.LINE_2, address.getAddressLine2());
        addressJson.put(PaymentFields.CITY, address.getCity());
        addressJson.put(PaymentFields.STATE, address.getState());
        addressJson.put(PaymentFields.ZIP_CODE, address.getZip());

        if (Objects.nonNull(country)) {
            addressJson.put(PaymentFields.COUNTRY, country.getName());
        }

        return addressJson;
    }

    /**
     * Constructs a JSON object representing a Customer (Payer), based on provided Payer model.
     * Maps customer attributes such as full name, email, and phone number (with country code)
     * into JSON fields expected by PayU API.
     *
     * @param payer the Payer model object containing customer information
     * @return a JsonObject properly formatted for PayU API customer input
     */
    private JsonObject buildCustomerJson(final Payer payer) {
        final JsonObject customerJson = Json.object();

        customerJson.put(PaymentFields.NAME, payer.getFullName());
        customerJson.put(PaymentFields.EMAIL, payer.getEmail());
        customerJson.put(PaymentFields.PHONE, payer.getPhoneWithCountryCode());

        return customerJson;
    }

    /**
     * Provides headers required for PayU API requests.
     *
     * @return map of header names and values
     */
    private Map<String, String> getDefaultHeaders(final String scope) {
        return Map.of(WebExchange.Header.AUTHORIZATION.getName(), String.format("Bearer %s", getAccessToken(scope)),
                WebExchange.Header.CONTENT_TYPE.getName(), WebExchange.ContentType.JSON_CONTENT.getName(),
                PaymentFields.MERCHANT_ID, merchantId);
    }
}

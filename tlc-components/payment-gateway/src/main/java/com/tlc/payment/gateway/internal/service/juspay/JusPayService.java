package com.tlc.payment.gateway.internal.service.juspay;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;

import com.tlc.payment.gateway.internal.fields.PaymentFields;
import com.tlc.payment.gateway.internal.manager.juspay.JusPayPaymentManager;
import com.tlc.payment.gateway.internal.status.PaymentGatewayErrorCodes;
import com.tlc.payment.gateway.manager.PaymentManager;
import com.tlc.payment.gateway.models.CardInfo;
import com.tlc.payment.gateway.models.CardType;
import com.tlc.payment.gateway.models.NetBankingInfo;
import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PayerLocalDetails;
import com.tlc.payment.gateway.models.Payment;
import com.tlc.payment.gateway.models.PaymentMethodInfo;
import com.tlc.payment.gateway.models.PaymentResponse;
import com.tlc.payment.gateway.models.PaymentMetaData;
import com.tlc.payment.gateway.models.PaymentMethod;
import com.tlc.payment.gateway.models.PaymentStatus;
import com.tlc.payment.gateway.models.BillingAmount;
import com.tlc.payment.gateway.models.UPIInfo;
import com.tlc.payment.gateway.models.WalletInfo;
import com.tlc.payment.gateway.service.PaymentService;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebExchange;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Handles JusPay gateway payment operations: creating/destroying payers, managing payment methods,
 * confirming payments, and mapping gateway responses to domain objects.
 * <p>
 * Implements {@link PaymentService} and orchestrates API calls to JusPay REST endpoints
 * and local data management through {@link PaymentManager}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class JusPayService implements PaymentService {

    private static String jusPayBaseUrl;
    private static String apiKey;

    private final PaymentManager paymentManager;

    public JusPayService() {
        paymentManager = JusPayPaymentManager.getInstance();
    }

    @Override
    public void start(final Configurations configurations) {
        jusPayBaseUrl = configurations.getConfig(PaymentFields.BASE_URL);
        apiKey = configurations.getConfig(PaymentFields.API_KEY);
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
        final URL url = buildURL(PaymentFields.ACCOUNT, PaymentFields.PAYMENT_METHODS);
        final HttpRequest httpRequest = new HttpRequest(url, Collections.emptyMap(), HttpMethod.GET, getDefaultHeaders());
        final JsonObject response = HttpUtil.makeRequest(httpRequest);

        return convertJsonIntoPaymentMethods(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param payment The payment object containing payment details.
     * @return A response object with response details of the payment.
     */
    @Override
    public PaymentResponse makePayment(final Payment payment) {
        throw ErrorCode.get(ErrorCodes.UNSUPPORTED_OPERATION_EXCEPTION);
    }

    /**
     * {@inheritDoc}
     *
     * @param payment The payment object containing payment details to be confirmed.
     * @return A response object with confirmation details of the payment.
     */
    @Override
    public PaymentResponse confirmPayment(final Payment payment) {
        final BillingAmount billingAmount = payment.getPrice();
        final JsonObject data = Json.object()
                .put(PaymentFields.AMOUNT, billingAmount.getTotalAmount())
                .put(PaymentFields.CURRENCY, billingAmount.getCurrency().getCode());

        final URL url = buildURL(PaymentFields.PAYMENTS, payment.getId(), PaymentFields.CAPTURE);
        final HttpRequest httpRequest = new HttpRequest(url, Map.of(PaymentFields.REQUEST_BODY, data.toString()), HttpMethod.POST, getDefaultHeaders());
        final JsonObject response = HttpUtil.makeRequest(httpRequest);

        return convertJsonIntoPaymentResponse(response);
    }

    /**
     * {@inheritDoc}
     *
     * @param payer The payer object containing details of the payer to be created.
     */
    @Override
    public void createPayer(final Payer payer) {
        final URL url = buildURL(PaymentFields.CUSTOMERS);
        final JsonObject data = Json.object()
                .put(PaymentFields.NAME, payer.getFullName())
                .put(PaymentFields.EMAIL, payer.getEmail())
                .put(PaymentFields.PHONE, payer.getPhone())
                .put(PaymentFields.PHONE_COUNTRY_CODE, payer.getPhoneCountryCode());

        final JsonObject jsonObject = HttpUtil.makeRequest(new HttpRequest(url, Map.of(PaymentFields.REQUEST_BODY, data.toString()), HttpMethod.POST, getDefaultHeaders()));

        payer.setRefenceId(jsonObject.getString(PaymentFields.ID));
        paymentManager.createPayer(payer);
    }

    /**
     * Deletes an existing payer from the system.
     *
     * @param payer The payer object containing details of the payer to be deleted.
     */
    @Override
    public void deletePayer(final Payer payer) {
        final URL url = buildURL(PaymentFields.CUSTOMERS, payer.getRefenceId());

        HttpUtil.makeRequest(new HttpRequest(url, Collections.emptyMap(), HttpMethod.DELETE, getDefaultHeaders()));
        paymentManager.deletePayer(payer);
    }

    /**
     * {@inheritDoc}
     *
     * @param payerLocalDetails Holds the details of local payer.
     * @return Collection of stored payment type info of the specified payer.
     */
    @Override
    public Collection<PaymentMethodInfo> getUsersPaymentMethods(final PayerLocalDetails payerLocalDetails) {
        final String payerId = paymentManager.getPayerReferenceId(payerLocalDetails);
        final URL url = buildURL(PaymentFields.CUSTOMERS, payerId, PaymentFields.PAYMENT_METHODS);
        final JsonObject jsonObject = HttpUtil.makeRequest(new HttpRequest(url, Collections.emptyMap(), HttpMethod.GET, getDefaultHeaders()));

        return convertJsonIntoStoredPaymentMethods(jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param paymentMetaData Holds the details of payment.
     * @return The stored payment type info associated with the given token.
     */
    @Override
    public PaymentMethodInfo getUsersPaymentMethod(final PaymentMetaData paymentMetaData) {
        final Collection<PaymentMethodInfo> storedPaymentMethods = getUsersPaymentMethods(paymentMetaData.getLocalDetails());
        final String token = paymentMetaData.getPayment().getToken();

        return storedPaymentMethods.stream().filter(storedPaymentMethod -> Objects.equals(storedPaymentMethod.getToken(), token))
                .findFirst().orElseGet(() -> null);
    }

    /**
     * Utility to build URLs for JusPay API requests.
     *
     * @param pathElements parts of the API path
     * @return constructed URL object
     * @throws ErrorCode if malformed URL
     */
    private URL buildURL(final String... pathElements) {
        final StringJoiner stringJoiner = new StringJoiner(StringUtil.Delimiter.FORWARD_SLASH.getDelimiter());

        stringJoiner.add(jusPayBaseUrl);

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
     * Converts a JusPay method JSON response into a collection of supported payment methods.
     *
     * @param jsonObject raw JSON response
     * @return Collection of {@link PaymentMethod} objects
     */
    private Collection<PaymentMethod> convertJsonIntoPaymentMethods(final JsonObject jsonObject) {
        final Collection<PaymentMethod> paymentMethods = new ArrayList<>();
        final JsonArray paymentMethodsArray = jsonObject.getJsonArray(PaymentFields.PAYMENT_METHODS);
        final int paymentMethodArraySize = paymentMethodsArray.size();

        for (int paymentMethodStartIndex = 0; paymentMethodStartIndex < paymentMethodArraySize; paymentMethodStartIndex++) {
            final JsonObject paymentMethodJson = paymentMethodsArray.getJsonObject(paymentMethodStartIndex);
            final PaymentMethod.Type paymentMethodType = PaymentMethod.Type.getByName(paymentMethodJson.getString(PaymentFields.PAYMENT_METHOD));
            final JsonArray paymentMethodTypeArray = paymentMethodJson.getJsonArray(PaymentFields.PAYMENT_METHOD_TYPES);
            final int paymentMethodTypeArraySize = paymentMethodTypeArray.size();

            for (int paymentMethodTypeStartIndex = 0; paymentMethodTypeStartIndex < paymentMethodTypeArraySize; paymentMethodTypeStartIndex++) {
                final PaymentMethod.Builder paymentMethodBuilder = new PaymentMethod.Builder();
                final JsonObject paymentMethodTypeJson = paymentMethodTypeArray.getJsonObject(paymentMethodTypeStartIndex);

                paymentMethodBuilder.setName(paymentMethodTypeJson.getString(PaymentFields.PAYMENT_METHOD_TYPE));
                paymentMethodBuilder.setType(paymentMethodType);
                paymentMethods.add(paymentMethodBuilder.build());
            }
        }
        return paymentMethods;
    }

    /**
     * Converts a stored payment JSON list into a collection of {@link PaymentMethodInfo} objects.
     *
     * @param jsonObject raw JSON response
     * @return Collection of {@link PaymentMethodInfo} objects
     */
    private Collection<PaymentMethodInfo> convertJsonIntoStoredPaymentMethods(final JsonObject jsonObject) {
        final Collection<PaymentMethodInfo> paymentMethodInfoList = new ArrayList<>();
        final JsonArray jsonArray = jsonObject.getJsonArray(PaymentFields.CUSTOMER_PAYMENT_METHODS);
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final PaymentMethodInfo paymentMethodInfo = convertJsonIntoStoredPaymentMethod(jsonArray.getJsonObject(index));

            if (Objects.nonNull(paymentMethodInfo)) {
                paymentMethodInfoList.add(paymentMethodInfo);
            }
        }
        return paymentMethodInfoList;
    }

    /**
     * Converts a JSON object from JusPay to a domain-specific PaymentMethodInfo object.
     *
     * @param jsonObject input JSON describing single stored payment method
     * @return specialized PaymentMethodInfo object (CardInfo, WalletInfo, UPIInfo, NetBankingInfo)
     */
    private PaymentMethodInfo convertJsonIntoStoredPaymentMethod(final JsonObject jsonObject) {
        final PaymentMethod.Type paymentMethodType = PaymentMethod.Type.getByName(jsonObject.getString(PaymentFields.PAYMENT_METHOD));

        if (Objects.isNull(paymentMethodType)) {
            return null;
        }

        return switch (paymentMethodType) {
            case CARD -> convertJsonIntoCardInfo(jsonObject);
            case WALLET -> convertJsonIntoWalletInfo(jsonObject);
            case UPI -> convertJsonIntoUPIInfo(jsonObject);
            case NET_BANKING -> convertJsonIntoNetBankingInfo(jsonObject);
        };
    }

    /**
     * Converts the input JSON object into a {@link CardInfo} domain object.
     * <p>
     * Extracts all relevant card details such as token, number, expiry, cardholder name,
     * issuer, provider, default status, and network, mapping JusPay fields into the domain model.
     * </p>
     *
     * @param jsonObject the JSON object representing card information from JusPay
     * @return a populated {@link CardInfo} instance
     */
    private CardInfo convertJsonIntoCardInfo(final JsonObject jsonObject) {
        final JsonObject cardJson = jsonObject.getJsonObject(PaymentFields.CARD);

        return new CardInfo.Builder()
                .id(jsonObject.getString(PaymentFields.PAYMENT_METHOD_ID))
                .token(jsonObject.getString(PaymentFields.PAYMENT_TOKEN))
                .issuer(jsonObject.getString(PaymentFields.PAYMENT_METHOD_ISSUER))
                .provider(jsonObject.getString(PaymentFields.PAYMENT_METHOD_TYPE))
                .number(cardJson.getString(PaymentFields.LAST_FOUR_DIGIT))
                .cardHolderName(cardJson.getString(PaymentFields.CARD_HOLDER_NAME))
                .expiryMonth(cardJson.getString(PaymentFields.EXPIRY_MONTH))
                .expiryYear(cardJson.getString(PaymentFields.EXPIRY_YEAR))
                .isDefault(cardJson.getBoolean(PaymentFields.IS_DEFAULT))
                .network(cardJson.getString(PaymentFields.CARD_NETWORK))
                .type(CardType.getByName(cardJson.getString(PaymentFields.CARD_TYPE)))
                .build();
    }

    /**
     * Converts the input JSON object into a {@link UPIInfo} domain object.
     * <p>
     * Maps JusPay UPI payment details, such as VPA, token, default status, and channel,
     * into the application’s {@link UPIInfo} model for payment processing and storage.
     * </p>
     *
     * @param jsonObject the JSON object representing UPI payment information
     * @return a populated {@link UPIInfo} instance
     */
    private UPIInfo convertJsonIntoUPIInfo(final JsonObject jsonObject) {
        final JsonObject upiJson = jsonObject.getJsonObject(PaymentFields.UPI);

        return new UPIInfo.Builder()
                .id(jsonObject.getString(PaymentFields.PAYMENT_METHOD_ID))
                .token(jsonObject.getString(PaymentFields.PAYMENT_TOKEN))
                .channel(jsonObject.getString(PaymentFields.PAYMENT_METHOD_TYPE))
                .vpa(upiJson.getString(PaymentFields.VPA))
                .isDefault(upiJson.getBoolean(PaymentFields.IS_DEFAULT))
                .build();
    }

    /**
     * Converts the input JSON object into a {@link WalletInfo} domain object.
     * <p>
     * Extracts relevant wallet payment details including token, wallet name,
     * provider, mobile number, and default status to map JusPay wallet data into the internal model.
     * </p>
     *
     * @param jsonObject the JSON object representing wallet payment information
     * @return a populated {@link  WalletInfo} instance
     */
    private WalletInfo convertJsonIntoWalletInfo(final JsonObject jsonObject) {
        final JsonObject walletJson = jsonObject.getJsonObject(PaymentFields.WALLET);

        return new WalletInfo.Builder()
                .id(jsonObject.getString(PaymentFields.PAYMENT_METHOD_ID))
                .token(jsonObject.getString(PaymentFields.PAYMENT_TOKEN))
                .name(walletJson.getString(PaymentFields.NAME))
                .mobileNumber(walletJson.getString(PaymentFields.MOBILE_NUMBER))
                .setProvider(walletJson.getString(PaymentFields.PROVIDER))
                .isDefault(walletJson.getBoolean(PaymentFields.IS_DEFAULT))
                .build();
    }

    /**
     * Converts the input JSON object into a {@link NetBankingInfo} domain object.
     * <p>
     * Maps JusPay net banking payment information including accountant name,
     * account reference, bank name, token, and default status, into the NetBankingInfo domain model.
     * </p>
     *
     * @param jsonObject the JSON object representing net banking payment info
     * @return a populated {@link NetBankingInfo} instance
     */
    private NetBankingInfo convertJsonIntoNetBankingInfo(final JsonObject jsonObject) {
        final JsonObject netBankingJson = jsonObject.getJsonObject(PaymentFields.NET_BANKING);

        return new NetBankingInfo.Builder()
                .id(jsonObject.getString(PaymentFields.PAYMENT_METHOD_ID))
                .token(jsonObject.getString(PaymentFields.PAYMENT_TOKEN))
                .accountantName(netBankingJson.getString(PaymentFields.ACCOUNTANT_NAME))
                .accountReference(netBankingJson.getString(PaymentFields.ACCOUNT_REFERENCE))
                .bankName(netBankingJson.getString(PaymentFields.BANK_NAME))
                .isDefault(netBankingJson.getBoolean(PaymentFields.IS_DEFAULT))
                .build();
    }

    /**
     * Converts a JusPay JSON confirmation response into a domain {@link PaymentResponse} object.
     *
     * @param jsonObject JusPay confirmation response
     * @return mapped {@link PaymentResponse}
     */
    private PaymentResponse convertJsonIntoPaymentResponse(final JsonObject jsonObject) {
        final PaymentResponse.Builder paymentResponseBuilder = new PaymentResponse.Builder();
        final Optional<PaymentStatus> optionalStatus = PaymentStatus.getByName(jsonObject.getString(PaymentFields.STATUS));

        optionalStatus.ifPresent(paymentResponseBuilder::setStatus);
        return paymentResponseBuilder.build();
    }

    /**
     * Provides headers required for JusPay API requests.
     *
     * @return map of header names and values
     */
    private Map<String, String> getDefaultHeaders() {
        return Map.of(WebExchange.Header.API_KEY.getName(), apiKey,
                WebExchange.Header.CONTENT_TYPE.getName(), WebExchange.ContentType.JSON_CONTENT.getName());
    }
}

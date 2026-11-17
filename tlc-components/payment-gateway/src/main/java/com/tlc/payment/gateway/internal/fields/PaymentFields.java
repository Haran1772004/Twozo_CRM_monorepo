package com.tlc.payment.gateway.internal.fields;

/**
 * The {@code PaymentFields} class centralizes constant field names used
 * throughout the payment gateway module.
 * <p>
 * These string constants ensure consistent usage and avoid typographical errors
 * when referencing keys and attribute names for payment processing, customer
 * data, transaction details, and merchant configuration.
 * </p>
 * <p>
 * Typical use cases include forming request parameters, mapping JSON fields,
 * and ensuring cross-service field compatibility in payment-related workflows.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class PaymentFields {

    public static final String ACCOUNT = "account";
    public static final String PAYMENT_METHODS = "payment_methods";
    public static final String AMOUNT = "amount_to_capture";
    public static final String PAYMENT_METHOD_ID = "payment_method_id";
    public static final String CURRENCY = "currency";
    public static final String LAST_FOUR_DIGIT = "last4_digits";
    public static final String PAYMENTS = "payments";
    public static final String CAPTURE = "capture";
    public static final String CUSTOMERS = "customers";
    public static final String ID = "id";
    public static final String MOBILE_NUMBER = "mobile_number";
    public static final String NAME = "name";
    public static final String PHONE_COUNTRY_CODE = "phone_country_code";
    public static final String EMAIL = "email";
    public static final String PHONE = "phone";
    public static final String BASE_URL = "baseUrl";
    public static final String ADDRESS = "address";
    public static final String API_KEY = "apiKey";
    public static final String CUSTOMER_PAYMENT_METHODS = "customer_payment_methods";
    public static final String PAYMENT_METHOD_TYPE = "payment_method_type";
    public static final String VPA = "vpa";
    public static final String WALLET = "wallet";
    public static final String CARD = "card";
    public static final String CARD_TYPE = "card_type";
    public static final String PAYMENT_METHOD_ISSUER = "payment_method_issuer";
    public static final String EXPIRY_MONTH = "expiry_month";
    public static final String PROVIDER = "provider";
    public static final String EXPIRY_YEAR = "expiry_year";
    public static final String CARD_HOLDER_NAME = "card_holder_name";
    public static final String PAYMENT_METHOD = "payment_method";
    public static final String PAYMENT_TOKEN = "payment_token";
    public static final String CARD_NETWORK = "card_network";
    public static final String IS_DEFAULT = "default_payment_method_set";
    public static final String UPI = "upi";
    public static final String NET_BANKING = "net_banking";
    public static final String ACCOUNTANT_NAME = "accountant_name";
    public static final String ACCOUNT_REFERENCE = "account_reference";
    public static final String BANK_NAME = "bank_name";
    public static final String STATUS = "status";
    public static final String PAYMENT_METHOD_TYPES = "payment_method_types";
    public static final String VIA_SMS = "viaSms";
    public static final String VIA_EMAIL = "viaEmail";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String TXN_ID = "txnid";
    public static final String UDF = "udf";
    public static final String UDF1 = "udf1";
    public static final String UDF2 = "udf2";
    public static final String REQUEST_BODY = "requestBody";
    public static final String MAX_PAYMENTS_ALLOWED = "maxPaymentsAllowed";
    public static final String LINE_1 = "line1";
    public static final String LINE_2 = "line2";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String COUNTRY = "country";
    public static final String ZIP_CODE = "zipCode";
    public static final String RESULT = "result";
    public static final String MERCHANT_ID = "mid";
    public static final String MERCHANT_DOT_ID = "merchant.id";
    public static final String URL = "url";
    public static final String PAYMENT_LINK = "paymentLink";
    public static final String TOKEN_GENERATION = "tokenGeneration";
    public static final String CLIENT_DOT_ID = "client.id";
    public static final String CLIENT_DOT_SECRET = "client.secret";
    public static final String OAUTH = "oauth";
    public static final String TOKEN = "token";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String SCOPE = "scope";
    public static final String GRANT_TYPE = "grant_type";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String CREATE_PAYMENT_LINKS = "create_payment_links";
    public static final String IS_AMOUNT_FILLED_BY_CUSTOMER = "isAmountFilledByCustomer";
    public static final String IS_PARTIAL_PAYMENT_ALLOWED = "isPartialPaymentAllowed";
    public static final String SOURCE = "source";
    public static final String DESCRIPTION = "description";
    public static final String DISCOUNT = "discount";
    public static final String TAX = "tax";
    public static final String SUB_AMOUNT = "subAmount";
    public static final String CUSTOMER = "customer";
    public static final String API = "API";
    public static final String PAYMENT_LINKS = "payment-links";
    public static final String SUCCESS_URL = "successURL";
    public static final String FAILURE_URL = "failureURL";
    public static final String REDIRECTION = "redirection";
    public static final String CLIENT_REDIRECTION = "client.redirection";
    public static final String EXPIRY_DATE = "expiryDate";
}

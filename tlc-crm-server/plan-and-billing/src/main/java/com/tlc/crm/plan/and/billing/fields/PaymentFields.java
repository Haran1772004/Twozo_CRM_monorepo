package com.tlc.crm.plan.and.billing.fields;

/**
 * Constants for all field names used in payment-gateway API models and data flows.
 *
 * <p>
 * Centralizes JSON key strings and field identifiers used in serialization,
 * deserialization, validation, and client/server message contracts for the
 * payment gateway. Ensures consistency and prevents typographical
 * errors when referencing fields throughout the application.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class PaymentFields {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String PAYMENT_TYPE = "paymentType";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String DATA = "data";
    public static final String MESSAGE = "message";
    public static final String PAYMENT_METHODS = "paymentMethods";
    public static final String ORG_ID = "orgId";
    public static final String USER_IDS = "userIds";
    public static final String TRANSACTION_ID = "transactionId";
    public static final String CHECKOUT_URL = "checkoutUrl";
    public static final String PAYMENT_GATEWAY = "payment-gateway";
    public static final String NEED_PAYER_CREATION = "needPayerCreation";
    public static final String URL = "url";

    private PaymentFields() {
    }
}

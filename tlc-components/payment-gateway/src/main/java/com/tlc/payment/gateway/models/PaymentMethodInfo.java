package com.tlc.payment.gateway.models;

/**
 * Defines the contract for payment type information.
 *
 * @author Sathish
 * @version 1.0
 */
public interface PaymentMethodInfo {

    String getId();

    PaymentMethod.Type getPaymentType();

    Status getStatus();

    String getToken();

    boolean isDefault();

    enum Status {

        ACTIVE, INACTIVE, EXPIRED, LINKED
    }
}

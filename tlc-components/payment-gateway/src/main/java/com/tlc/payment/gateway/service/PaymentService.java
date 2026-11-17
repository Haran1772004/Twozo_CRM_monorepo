package com.tlc.payment.gateway.service;

import com.tlc.commons.service.Service;

import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PayerLocalDetails;
import com.tlc.payment.gateway.models.Payment;
import com.tlc.payment.gateway.models.PaymentMetaData;
import com.tlc.payment.gateway.models.PaymentMethod;
import com.tlc.payment.gateway.models.PaymentMethodInfo;
import com.tlc.payment.gateway.models.PaymentResponse;

import java.util.Collection;

/**
 * Interface defining the payment service operations within the payment gateway.
 * Provides methods for handling payment methods, confirming payments, and managing payers.
 *
 * @author Sathish
 * @version 1.0
 */
public interface PaymentService extends Service {

    /**
     * Retrieves a collection of available payment methods.
     *
     * @return Collection of payment methods supported by the gateway.
     */
    Collection<PaymentMethod> getPaymentMethods();

    /**
     * Makes a payment transaction.
     *
     * @param payment The payment object containing payment details to be confirmed.
     * @return A response object with confirmation details of the payment.
     */
    PaymentResponse makePayment(final Payment payment);

    /**
     * Confirms a payment transaction.
     *
     * @param payment The payment object containing payment details to be confirmed.
     * @return A response object with confirmation details of the payment.
     */
    PaymentResponse confirmPayment(final Payment payment);

    /**
     * Creates a new payer in the provider.
     *
     * @param payer The payer object containing details of the payer to be created.
     */
    void createPayer(final Payer payer);

    /**
     * Deletes an existing payer from the provider.
     *
     * @param payer The payer object containing details of the payer to be deleted.
     */
    void deletePayer(final Payer payer);

    /**
     * Retrieves user stored payment methods associated with a given payer.
     *
     * @param payerLocalDetails Holds the details of local payer.
     * @return Collection of stored payment type info of the specified payer.
     */
    Collection<PaymentMethodInfo> getUsersPaymentMethods(final PayerLocalDetails payerLocalDetails);

    /**
     * Retrieves a user stored payment method by its token identifier.
     *
     * @param paymentMetaData Holds the details of payment.
     * @return The stored payment type info associated with the given token.
     */
    PaymentMethodInfo getUsersPaymentMethod(final PaymentMetaData paymentMetaData);
}
package com.tlc.payment.gateway.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.payment.gateway.models.Payer;
import com.tlc.payment.gateway.models.PayerLocalDetails;

/**
 * Defines the contract for managing payment-related operations including
 * payer creation, deletion, and retrieval of stored payment methods,
 * as well as payment confirmation processes.
 * <p>
 * Implementations provide the business logic for interacting with payment
 * services and data stores to perform these operations.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PaymentManager {

    /**
     * Creates a new payer in the system.
     *
     * @param payer The payer object containing details of the payer to be created.
     */
    void createPayer(final Payer payer);

    /**
     * Deletes an existing payer from the system.
     *
     * @param payer The payer object containing details of the payer to be deleted.
     */
    void deletePayer(final Payer payer);

    /**
     * Retrieves the payer reference ID based on local payer details.
     *
     * @param payerLocalDetails details used to identify the payer
     * @return reference ID string
     * @throws ErrorCode if payer is not found
     */
    String getPayerReferenceId(final PayerLocalDetails payerLocalDetails);
}

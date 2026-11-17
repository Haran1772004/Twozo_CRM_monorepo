package com.tlc.payment.gateway.models;

import com.tlc.entity.model.TwozoApplication;

/**
 * Encapsulates local identification details for a payer in the payment gateway domain.
 *
 * @author Sathish
 * @version 1.0
 */
public interface PayerLocalDetails {

    /**
     * Returns the application name associated with the payer's local identity.
     *
     * @return the application name
     */
    TwozoApplication getApplication();

    /**
     * Returns the identity type used in payer identification.
     *
     * @return the identity type string
     */
    IdentityType getIdentityType();

    /**
     * Returns the local identity value for the payer.
     *
     * @return the payer's local identity string
     */
    String getLocalIdentity();

    /**
     * Represents the type of the local identity.
     */
    interface IdentityType {

        String getName();
    }
}

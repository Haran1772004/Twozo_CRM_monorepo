package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.payment.gateway.models.BillingAmount;
import com.tlc.payment.gateway.models.Payer;

/**
 * Model class representing billing details, including contact and billingAmount information.
 * Encapsulates a Payer contact and BillingAmount details associated with a billing event.
 *
 * @author Sathish
 * @version 1.0
 */
public class BillingDetails {

    private Payer contact;
    private BillingAmount billingAmount;
    private Long localTransactionId;
    private String transactionId;

    public Payer getContact() {
        return contact;
    }

    public void setContact(final Payer contact) {
        this.contact = contact;
    }

    public BillingAmount getPrice() {
        return billingAmount;
    }

    public void setPrice(final BillingAmount billingAmount) {
        this.billingAmount = billingAmount;
    }

    public Long getLocalTransactionId() {
        return localTransactionId;
    }

    public void setLocalTransactionId(final Long localTransactionId) {
        this.localTransactionId = localTransactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }
}

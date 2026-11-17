package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.payment.gateway.models.PaymentStatus;

/**
 * Simple data transfer object representing a subscription response.
 * Contains the status of the subscription and the URL for checkout.
 *
 * @author Sathish
 * @version 1.0
 */
public class SubscriptionResponse {

    private String checkoutUrl;
    private PaymentStatus status;

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(final PaymentStatus status) {
        this.status = status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(final String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }
}

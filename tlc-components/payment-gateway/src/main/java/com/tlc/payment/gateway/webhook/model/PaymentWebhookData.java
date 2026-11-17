package com.tlc.payment.gateway.webhook.model;

import com.tlc.commons.data.model.ConsumableData;
import com.tlc.payment.gateway.models.PaymentStatus;

/**
 * Represents the data payload for a payment webhook event.
 * Contains transaction identification and payment status information.
 * Implements the ConsumableData marker interface for webhook data types.
 *
 * @author Sathish
 * @version 1.0
 */
public class PaymentWebhookData implements ConsumableData {

    private Long localTransactionId;
    private String transactionId;
    private String allowedOrigin;
    private PaymentStatus paymentStatus;

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

    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    public void setAllowedOrigin(final String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(final PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}

package com.tlc.payment.gateway.models;

/**
 * Represents the response received after confirming a payment transaction.
 *
 * @author Sathish
 * @version 1.0
 */
public class PaymentResponse {

    private final String checkoutUrl;
    private final PaymentStatus status;

    private PaymentResponse(final PaymentStatus status, final String checkoutUrl) {
        this.status = status;
        this.checkoutUrl = checkoutUrl;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public static class Builder {

        private String checkoutUrl;
        private PaymentStatus status;

        public Builder setStatus(final PaymentStatus status) {
            this.status = status;
            return this;
        }

        public Builder setCheckoutURL(final String checkoutUrl) {
            this.checkoutUrl = checkoutUrl;
            return this;
        }

        public PaymentResponse build() {
            return new PaymentResponse(status, checkoutUrl);
        }
    }
}

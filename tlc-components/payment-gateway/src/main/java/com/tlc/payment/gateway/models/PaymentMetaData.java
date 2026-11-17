package com.tlc.payment.gateway.models;

/**
 * Holds supplementary data required for payment transaction processing.
 *
 * @author Sathish
 * @version 1.0
 */
public class PaymentMetaData {

    private String payerId;
    private Payment payment;
    private PayerLocalDetails localDetails;

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(final Payment payment) {
        this.payment = payment;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(final String payerId) {
        this.payerId = payerId;
    }

    public PayerLocalDetails getLocalDetails() {
        return localDetails;
    }

    public void setLocalDetails(final PayerLocalDetails localDetails) {
        this.localDetails = localDetails;
    }
}

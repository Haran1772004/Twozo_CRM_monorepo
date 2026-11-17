package com.tlc.payment.gateway.models;

/**
 * Represents a payment transaction in the payment gateway system.
 *
 * @author Sathish
 * @version 1.0
 */
public class Payment {

    private String id;
    private String orderItem;
    private String token;
    private String transactionId;
    private Long localTransactionId;
    private Payer payer;
    private BillingAmount billingAmount;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(final String orderItem) {
        this.orderItem = orderItem;
    }

    public BillingAmount getPrice() {
        return billingAmount;
    }

    public void setPrice(final BillingAmount billingAmount) {
        this.billingAmount = billingAmount;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public void setPayer(final Payer payer) {
        this.payer = payer;
    }

    public Payer getPayer() {
        return payer;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getLocalTransactionId() {
        return localTransactionId;
    }

    public void setLocalTransactionId(final Long localTransactionId) {
        this.localTransactionId = localTransactionId;
    }
}

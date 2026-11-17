package com.tlc.payment.gateway.models;

import com.tlc.l10n.model.currency.Currency;

import java.util.Objects;

/**
 * Represents the pricing details of a payment including amount, tax, discount,
 * convenience fee, total amount, and currency.
 * Contains getter and setter methods for each field.
 * Provides computed total amount if not explicitly set.
 *
 * @author Sathish
 * @version 1.0
 */
public class BillingAmount {

    private double amount;
    private double tax;
    private double discount;
    private double convenienceFee;
    private Double totalAmount;
    private Currency currency;

    public double getAmount() {
        return amount;
    }

    public void setAmount(final double amount) {
        this.amount = amount;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(final double tax) {
        this.tax = tax;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(final double discount) {
        this.discount = discount;
    }

    public double getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(final double convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    /**
     * Returns the total amount considering the amount, tax, convenience fee, and discount.
     * If totalAmount is explicitly set, returns that value instead.
     *
     * @return Calculated or explicit total amount
     */
    public double getTotalAmount() {
        if (Objects.isNull(totalAmount)) {
            return totalAmount = amount + tax + convenienceFee - discount;
        }
        return totalAmount;
    }
}

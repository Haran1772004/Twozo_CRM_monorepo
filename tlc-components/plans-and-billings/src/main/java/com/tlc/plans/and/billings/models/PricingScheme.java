package com.tlc.plans.and.billings.models;

import com.tlc.l10n.model.currency.Currency;

import java.util.Collection;
import java.util.Objects;

/**
 * Represents a pricing scheme for a subscription plan.
 *
 * @author Sathish
 * @version 1.0
 */
public class PricingScheme {

    private Long id;
    private double value;
    private Discounts discounts;
    private PricingBasis pricingBasis;
    private BillingCycle billingCycle;
    private Currency baseCurrency;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(final double value) {
        this.value = value;
    }

    public Discounts getDiscounts() {
        return discounts;
    }

    public void setDiscounts(final Discounts discounts) {
        this.discounts = discounts;
    }

    public void addDiscounts(final Collection<Discount> discounts) {
        this.discounts = Objects.isNull(this.discounts) ? new Discounts() : this.discounts;

        this.discounts.addAll(discounts);
    }

    public void addDiscount(final Discount discount) {
        discounts = Objects.isNull(discounts) ? new Discounts() : discounts;

        discounts.add(discount);
    }

    public PricingBasis getPricingBasis() {
        return pricingBasis;
    }

    public void setPricingBasis(final PricingBasis pricingBasis) {
        this.pricingBasis = pricingBasis;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(final BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(final Currency baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
}

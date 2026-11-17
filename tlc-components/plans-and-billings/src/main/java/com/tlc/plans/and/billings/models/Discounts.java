package com.tlc.plans.and.billings.models;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents discount entities and related business logic.
 *
 * @author Sathish
 * @version 1.0
 */
public class Discounts {

    private final Collection<Discount> discounts;

    public Discounts() {
        discounts = new HashSet<>();
    }

    public Discounts(final Collection<Discount> discounts) {
        this.discounts = new HashSet<>(discounts);
    }

    public void addAll(final Collection<Discount> discounts) {
        this.discounts.addAll(discounts);
    }

    public void add(final Discount discount) {
        discounts.add(discount);
    }

    public Collection<Discount> getAll() {
        return discounts;
    }

    public double getDiscountedValue(final double actualPrice) {
        double discountValue = 0;

        for (final Discount discount : discounts) {
            discountValue += discount.getValue() * actualPrice;
        }
        return Math.round((actualPrice - discountValue) * 10.0) / 10.0;
    }
}

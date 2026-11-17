package com.tlc.crm.core.features.sales.template.model;

import java.math.BigDecimal;

/**
 * <p>
 * Represents a product line item with its corresponding monetary amount.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class LineItem {

    private Long productId;
    private String name;
    private double amount;

    public LineItem() {
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(final Long productId) {
        this.productId = productId;
    }

    public double getAmount() {
        return amount;
    }

    /**
     * <p>
     * Sets the monetary amount for this line item.
     * </p>
     *
     * @param amount Represents the line item amount as a {@link BigDecimal}.
     */
    public void setAmount(final double amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}

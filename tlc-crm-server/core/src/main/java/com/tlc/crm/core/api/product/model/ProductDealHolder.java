package com.tlc.crm.core.api.product.model;

import java.util.Collection;

/**
 * Model which represents the {@link ProductDealHolder}
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ProductDealHolder {

    private Product product;
    private Collection<Long> dealIds;

    public Collection<Long> getDealIds() {
        return dealIds;
    }

    public Product getProduct() {
        return product;
    }

    public void setDealIds(final Collection<Long> dealIds) {
        this.dealIds = dealIds;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }
}

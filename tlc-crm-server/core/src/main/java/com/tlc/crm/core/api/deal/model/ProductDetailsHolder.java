package com.tlc.crm.core.api.deal.model;

import com.tlc.l10n.model.currency.Currency;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Model which represents the Product Detail Holder
 *
 * @author karthickV
 * @version 1.0
 */
public class ProductDetailsHolder implements TlcModel {

    @NotNull(groups = {Delete.class}, message = "i18n.productDetailsHolder.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.productDetailsHolder.invalid.orgId")
    private Long orgId;

    @NotNull(groups = {Update.class}, message = "i18n.productDetailsHolder.invalid.dealId")
    private Long dealId;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.productDetailsHolder.invalid.currency")
    private Currency currency;

    private boolean isTaxInclusive;

    @Valid
    private Collection<ProductDetail> productDetails;

    private boolean isManuallyAdded;

    public Currency getCurrency() {
        return currency;
    }

    public boolean isManuallyAdded() {
        return isManuallyAdded;
    }

    public void setManuallyAdded(final boolean manuallyAdded) {
        isManuallyAdded = manuallyAdded;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public boolean isTaxInclusive() {
        return isTaxInclusive;
    }

    public void setTaxInclusive(final boolean isTaxInclusive) {
        this.isTaxInclusive = isTaxInclusive;
    }

    public Long getDealId() {
        return dealId;
    }

    public void setDealId(final Long dealId) {
        this.dealId = dealId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Collection<ProductDetail> getProductDetails() {
        return productDetails;
    }

    public void setProductDetails(final Collection<ProductDetail> productDetails) {
        this.productDetails = productDetails;
    }

    public void addProductDetail(final ProductDetail productDetail) {
        this.productDetails = Objects.isNull(this.productDetails) ? new ArrayList<>() : this.productDetails;

        this.productDetails.add(productDetail);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Calculates the total amount for a product.
     *
     * @param productDetailsHolder {@link ProductDetailsHolder} An entity that holds the attributes which have to be saved into the database
     * @return {@link Double} The total amount of the product, including subtotal and tax
     */
    public double getTotalAmount(final ProductDetailsHolder productDetailsHolder) {
        return Math.round((getSubTotal(productDetailsHolder) + getTaxTotal(productDetailsHolder)) * 100.0) / 100.0;
    }

    /**
     * Calculates the total tax for a product.
     *
     * @param productDetailsHolder An entity that holds the attributes which have to be saved into the database.
     * @return {@link Double} The total tax for the product.
     */
    public double getTaxTotal(final ProductDetailsHolder productDetailsHolder) {
        double taxTotal = 0.0;
        final Collection<ProductDetail> productDetailsHolders = productDetailsHolder.getProductDetails();

        if (Objects.nonNull(productDetailsHolders) && !productDetailsHolders.isEmpty()) {

            for (final ProductDetail productDetail : productDetailsHolders) {

                if (productDetailsHolder.isTaxInclusive()) {
                    taxTotal += (productDetail.getAmount() * productDetail.getTax() / 100);
                }
            }
        }

        return taxTotal;
    }

    /**
     * Calculates the sub-total for a product.
     *
     * @param productDetailsHolder An entity that holds the attributes which have to be saved into the database.
     * @return {@link Double} The sub-total for the product.
     */
    public double getSubTotal(final ProductDetailsHolder productDetailsHolder) {
        double subTotal = 0.0;

        if (Objects.nonNull(productDetailsHolder)) {
            final Collection<ProductDetail> productDetails = productDetailsHolder.getProductDetails();

            if (Objects.nonNull(productDetails)) {

                for (final ProductDetail productDetail : productDetails) {
                    subTotal += productDetail.getAmount();
                }
            }
        }

        return subTotal;
    }

}
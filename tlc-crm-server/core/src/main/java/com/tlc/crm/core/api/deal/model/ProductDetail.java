package com.tlc.crm.core.api.deal.model;

import com.tlc.crm.core.api.product.model.Product;
import com.tlc.validator.TlcModel;
import com.tlc.crm.core.api.product.model.Variation;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * Model which represents the ProductDetails
 *
 * @author karthickV
 * @version 1.0
 */
public class ProductDetail implements TlcModel, Serializable {

    private Long id;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.price")
    @Digits(integer = 13, fraction = 3, groups = {Create.class, Update.class}, message = "i18n.productDetail.price.outOfBounds")
    private double price;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.quantity")
    private long quantity;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.minimum.discount")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.maximum.discount")
    private double discount;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.minimum.tax")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.maximum.tax")
    private double tax;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.amount")
    @Digits(integer = 14, fraction = 3, groups = {Create.class, Update.class}, message = "i18n.productDetail.amount.outOfBounds")
    private double amount;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.productDetail.invalid.product")
    private Product product;

    @Size(max = 200, groups = {Create.class, Update.class}, message = "i18n.productDetail.comment.limit.exceed")
    private String comment;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.productDetailsHolder.invalid.orgId")
    private Long orgId;

    private Variation variation;

    private Long productDetailsHolderId;

    private Long variationId;

    public void setId(final Long id) {
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(final double price) {
        this.price = price;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(final double tax) {
        this.tax = tax;
    }

    public Long getProductDetailsHolderId() {
        return productDetailsHolderId;
    }

    public void setProductDetailsHolderId(final Long productDetailsHolderId) {
        this.productDetailsHolderId = productDetailsHolderId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(final Product product) {
        this.product = product;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(final long quantity) {
        this.quantity = quantity;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(final double discount) {
        this.discount = discount;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(final double amount) {
        this.amount = amount;
    }

    public Long getVariationId() {
        return variationId;
    }

    public void setVariationId(final Long variationId) {
        this.variationId = variationId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public Long id() {
        return id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Variation getVariation() {
        return variation;
    }

    public void setVariation(final Variation variation) {
        this.variation = variation;
    }
}
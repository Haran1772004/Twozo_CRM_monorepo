package com.tlc.crm.core.api.product.model;

import com.tlc.l10n.model.currency.Currency;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PriceTag implements TlcModel {

    public static final String ENTITY_TYPE = "Price Tag";

    @NotNull(groups = {Delete.class}, message = "i18n.product.priceTag.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.product.currency.invalid")
    private Currency currency;

    @NotNull(groups = {Update.class}, message = "i18n.product.invalid.id")
    private Long productId;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.product.priceTag.invalid.price")
    @Digits(integer = 13, fraction = 3, groups = {Create.class, Update.class}, message = "i18n.product.priceTag.outOfBounds")
    private Double price;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    @NotNull(groups = {Update.class}, message = "i18n.product.variation.invalid.id")
    private Long variationId;

    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.product.priceTag.invalid.tax")
    private Double tax;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.product.priceTag.invalid.notnull.discount")
    @Max(value = 100, groups = {Create.class, Update.class}, message = "i18n.product.priceTag.invalid.maximum.discount")
    @Min(value = 0, groups = {Create.class, Update.class}, message = "i18n.product.priceTag.invalid.minimum.discount")
    private Double discount;

    @Override
    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    //TODO - Need to replace with Monetary
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getVariationId() {
        return variationId;
    }

    public void setVariationId(Long variationId) {
        this.variationId = variationId;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(final Double discount) {
        this.discount = discount;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}

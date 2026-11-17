package com.tlc.crm.core.api.product.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Model which represents the Variation
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class Variation implements TlcModel {

    public static final String DEFAULT = "default";

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.product.variation.invalid.id")
    private Long id;

    @Size(max = 50, groups = {Group.Create.class, Group.Update.class}, message = "i18n.product.variation.max.size")
    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.product.variation.invalid.name")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

    private Long productId;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.product.priceTag.invalid")
    @Valid
    private Collection<PriceTag> priceTags;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Collection<PriceTag> getPriceTags() {
        return this.priceTags;
    }

    public void addPriceTag(PriceTag priceTag) {
        this.priceTags = this.priceTags == null ? new ArrayList<>() : this.priceTags;

        this.priceTags.add(priceTag);
    }

    public void setPriceTags(Collection<PriceTag> priceTags) {
        this.priceTags = priceTags;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Object identity() {
        return null;
    }
}
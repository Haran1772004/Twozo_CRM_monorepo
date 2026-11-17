package com.tlc.crm.core.api.product.model;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class Category implements TlcModel {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.product.category.invalid.id")
    private Long id;

    @NotEmpty(groups = {Create.class, Update.class}, message = "i18n.product.category.invalid.name")
    private String name;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;

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

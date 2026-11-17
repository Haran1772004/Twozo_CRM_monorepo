package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.fields.models.FieldSource;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * A model that represents the provider data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterProvider {

    @NotNull(groups = {Group.Create.class}, message = "i18n.filter.provider.invalid.id")
    private Long id;
    private Provider provider;
    private FieldSource fieldSource;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(final Provider provider) {
        this.provider = provider;
    }

    public FieldSource getFieldSource() {
        return fieldSource;
    }

    public void setFieldSource(final FieldSource fieldSource) {
        this.fieldSource = fieldSource;
    }
}

package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.entity.models.type.DropdownType;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>
 * A model that represents the dropdown details data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DropdownDetails {

    private Long id;
    private String dropdownName;
    private String[] requiredFields;
    private short order;
    private boolean additionalOption;
    private boolean isStatic;
    private DropdownType dropdownType;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getDropdownName() {
        return dropdownName;
    }

    public void setDropdownName(final String dropdownName) {
        this.dropdownName = dropdownName;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(final short order) {
        this.order = order;
    }

    public String[] getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(final String[] requiredFields) {
        this.requiredFields = requiredFields;
    }

    public boolean isAdditionalOption() {
        return additionalOption;
    }

    public void setAdditionalOption(final boolean additionalOption) {
        this.additionalOption = additionalOption;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(final boolean isStatic) {
        this.isStatic = isStatic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || !(object instanceof final DropdownDetails dropdownDetails)) {
            return false;
        }

        return order == dropdownDetails.order && Objects.equals(id, dropdownDetails.id) && Objects.equals(dropdownName, dropdownDetails.dropdownName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, dropdownName, order, additionalOption) + Arrays.hashCode(requiredFields);
    }

    public DropdownType getDropdownType() {
        return dropdownType;
    }

    public void setDropdownType(final DropdownType dropdownType) {
        this.dropdownType = dropdownType;
    }
}

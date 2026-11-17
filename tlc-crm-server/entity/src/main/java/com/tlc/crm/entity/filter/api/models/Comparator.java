package com.tlc.crm.entity.filter.api.models;

import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * <p>
 * A model that represents the comparator data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Comparator {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.filter.criteria.comparator.invalid.id")
    private Long id;
    private Long specializationId;
    private I18nKey name;
    private String keyName;
    private String aliasName;
    private Specialization specialization;
    private int order;
    private boolean isDefault;
    private boolean isValueNeeded;
    private boolean isMultiValueAllowed;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public I18nKey getName() {
        return name;
    }

    public void setName(final I18nKey name) {
        this.name = name;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(final String keyName) {
        this.keyName = keyName;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setIsDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isValueNeeded() {
        return isValueNeeded;
    }

    public void setValueNeeded(final boolean isValueNeeded) {
        this.isValueNeeded = isValueNeeded;
    }

    public void setSpecializationId(final Long specializationId) {
        this.specializationId = specializationId;
    }

    public Long getSpecializationId() {
        return specializationId;
    }

    public void setSpecialization(final Specialization specialization) {
        this.specialization = specialization;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public boolean isMultiValueAllowed() {
        return isMultiValueAllowed;
    }

    public void setMultiValueAllowed(final boolean isMultiValueAllowed) {
        this.isMultiValueAllowed = isMultiValueAllowed;
    }

    public enum Specialization {
        TYPE, SUB_TYPE
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }
        final Comparator comparator = (Comparator) object;

        return order == comparator.order && isDefault == comparator.isDefault && isValueNeeded == comparator.isValueNeeded
                && Objects.equals(id, comparator.id) && Objects.equals(specializationId, comparator.specializationId)
                && Objects.equals(name.getKey(), comparator.name.getKey()) && Objects.equals(keyName, comparator.keyName)
                && Objects.equals(aliasName, comparator.aliasName) && specialization == comparator.specialization;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, specializationId, name.getKey(), keyName, aliasName, specialization, order, isDefault, isValueNeeded);
    }
}

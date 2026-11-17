package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.fields.models.FieldType;
import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * A model that represents the field data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class CriteriaField {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.filter.criteria.field.invalid.id")
    private Long id;
    private FieldType fieldType;
    private I18nKey name;
    private String aliasName;
    private Long sourceId;
    private AdditionalOption additionalOption;
    private Collection<DropdownDetails> dropdownDetails;
    private boolean hasValueDropdown;
    private boolean staticDropdown;
    private boolean isDefault;

    private final boolean isField;

    public CriteriaField() {
        this(true);
    }

    public CriteriaField(final boolean isField) {
        this.isField = isField;
    }

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

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(final FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public AdditionalOption getAdditionalOption() {
        return additionalOption;
    }

    public void setAdditionalOption(final AdditionalOption additionalOption) {
        this.additionalOption = additionalOption;
    }

    public Collection<DropdownDetails> getDropdownDetails() {
        return dropdownDetails;
    }

    public void addAllDropdownDetails(final Set<DropdownDetails> dropdownDetails) {
        if (Objects.isNull(this.dropdownDetails)) {
            this.dropdownDetails = new HashSet<>();
        }
        this.dropdownDetails.addAll(dropdownDetails);
    }

    public void addDropdownDetails(final DropdownDetails dropdownDetails) {
        if (Objects.isNull(this.dropdownDetails)) {
            this.dropdownDetails = new HashSet<>();
        }
        this.dropdownDetails.add(dropdownDetails);
    }

    public void setDropdownDetails(final Set<DropdownDetails> dropdownDetails) {
        this.dropdownDetails = dropdownDetails;
    }

    public boolean hasValueDropdown() {
        return hasValueDropdown;
    }

    public void setHasValueDropdown(final boolean hasValueDropdown) {
        this.hasValueDropdown = hasValueDropdown;
    }

    public boolean isField() {
        return isField;
    }

    public boolean hasStaticDropdown() {
        return staticDropdown || (Objects.nonNull(dropdownDetails) && dropdownDetails.stream().filter(dropdown ->
                !dropdown.isAdditionalOption()).findAny().map(DropdownDetails::isStatic).orElse(false));
    }

    public void setStaticDropdown(final boolean staticDropdown) {
        this.staticDropdown = staticDropdown;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(final boolean isDefault) {
        this.isDefault = isDefault;
    }
}

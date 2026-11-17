package com.tlc.crm.auto.assignment.api.model;

import com.tlc.i18n.I18nKey;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * A model that represents the {@link AutoAssignmentComparator} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentComparator {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.auto.assignment.comparator.id.required")
    private Long id;
    private I18nKey name;
    private String aliasName;
    private boolean hasValue;

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

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(final String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean hasValue() {
        return hasValue;
    }

    public void setHasValue(final boolean hasValue) {
        this.hasValue = hasValue;
    }

}

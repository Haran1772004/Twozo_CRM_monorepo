package com.tlc.crm.workflow.api.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * A model that represents the {@link CriteriaDropdownInfo} data.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class CriteriaDropdownInfo {

    private String dropdownName;
    private boolean isStatic;
    private Collection<String> requiredKeys;

    public String getDropdownName() {
        return dropdownName;
    }

    public void setDropdownName(final String dropdownName) {
        this.dropdownName = dropdownName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(final boolean aStatic) {
        isStatic = aStatic;
    }

    public Collection<String> getRequiredKeys() {
        return requiredKeys;
    }

    public void setRequiredKeys(final Collection<String> requiredKeys) {
        this.requiredKeys = requiredKeys;
    }

    public void addRequiredKey(final String requiredKey) {
        this.requiredKeys = this.requiredKeys == null ? new HashSet<>() : this.requiredKeys;
        this.requiredKeys.add(requiredKey);
    }
}

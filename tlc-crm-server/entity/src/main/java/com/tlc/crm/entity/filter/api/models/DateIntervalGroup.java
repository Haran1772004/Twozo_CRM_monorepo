package com.tlc.crm.entity.filter.api.models;

import com.tlc.i18n.I18nKey;

import java.util.Objects;

/**
 * <p>
 * A model that represents the date interval group data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DateIntervalGroup {

    private Long id;
    private I18nKey name;
    private boolean isRequired;

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

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(final boolean isRequired) {
        this.isRequired = isRequired;
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof DateIntervalGroup && this.hashCode() == object.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, isRequired);
    }
}

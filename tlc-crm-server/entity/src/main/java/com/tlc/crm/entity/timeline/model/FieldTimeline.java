package com.tlc.crm.entity.timeline.model;

import com.tlc.i18n.I18nKey;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Holds the existing and current state of a field, capturing existing values before changes,
 * the latest value after modification, and the name of the field in {@link I18nKey} format for multilingual support.
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public final class FieldTimeline {

    private final Collection<String> oldValues;
    private final Collection<String> newValues;
    private final I18nKey fieldName;
    private final String oldValue;

    public FieldTimeline(final Collection<String> oldValues, final Collection<String> newValues, final I18nKey fieldName) {
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.fieldName = fieldName;
        this.oldValue = Objects.isNull(oldValues) || oldValues.isEmpty() ? null : oldValues.iterator().next();
    }

    public FieldTimeline(final String newValue, final I18nKey fieldName) {
        this(null, List.of(newValue), fieldName);
    }

    public FieldTimeline(final Collection<String> oldValues, final String newValue, final I18nKey fieldName) {
        this(oldValues, Objects.isNull(newValue) ? Collections.emptyList() : List.of(newValue), fieldName);
    }

    public FieldTimeline(final String newValue) {
        this(null, List.of(newValue), null);
    }

    public I18nKey getFieldName() {
        return fieldName;
    }

    public Collection<String> getOldValues() {
        return oldValues;
    }

    public Collection<String> getNewValues() {
        return newValues;
    }

    /**
     * <p>
     * Retrieves the first old value from the {@link Collection} of existing old values.
     * </p>
     *
     * @return the first old value
     */
    public String getOldValue() {
        return oldValue;
    }
}
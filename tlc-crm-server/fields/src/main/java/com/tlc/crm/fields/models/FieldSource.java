package com.tlc.crm.fields.models;

import com.tlc.crm.fields.models.settings.SourceSettings;

import com.tlc.i18n.I18nKey;

/**
 * <p>
 * A model that represents a source
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class FieldSource {

    private Long id;
    private String name;
    private I18nKey displayName;
    private SourceSettings settings;
    private boolean isCommon;
    private boolean isImportable;

    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public SourceSettings getSettings() {
        return settings;
    }

    public void setSettings(final SourceSettings sourceSettings) {
        this.settings = sourceSettings;
    }

    public boolean isCommon() {
        return isCommon;
    }

    public void setCommon(final boolean isCommon) {
        this.isCommon = isCommon;
    }

    public boolean isImportable() {
        return isImportable;
    }

    public void setImportable(final boolean isImportable) {
        this.isImportable = isImportable;
    }
}

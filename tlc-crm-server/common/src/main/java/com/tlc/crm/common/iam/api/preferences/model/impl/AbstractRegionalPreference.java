package com.tlc.crm.common.iam.api.preferences.model.impl;

import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;
import com.tlc.crm.common.iam.api.preferences.model.RegionalPreference;
import com.tlc.crm.common.models.Language;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * Model represents the {@link AbstractRegionalPreference} details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractRegionalPreference<T extends PreferenceType> implements RegionalPreference<T> {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.regional.settings.invalid.timeZone")
    private TimezoneInfo timezoneInfo;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.regional.settings.invalid.language")
    private Language language;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.regional.settings.invalid.dateFormat")
    private DateFormat dateFormat;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.entity.regional.settings.invalid.timeFormat")
    private TimeFormat timeFormat;

    @Override
    public void setTimezoneInfo(final TimezoneInfo timezoneInfo) {
        this.timezoneInfo = timezoneInfo;
    }

    @Override
    public void setDateFormat(final DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public void setTimeFormat(final TimeFormat timeFormat) {
        this.timeFormat = timeFormat;
    }

    @Override
    public void setLanguage(final Language language) {
        this.language = language;
    }

    @Override
    public DateFormat getDateFormat() {
        return dateFormat;
    }

    @Override
    public TimeFormat getTimeFormat() {
        return timeFormat;
    }

    @Override
    public TimezoneInfo getTimezoneInfo() {
        return timezoneInfo;
    }

    @Override
    public Language getLanguage() {
        return language;
    }
}

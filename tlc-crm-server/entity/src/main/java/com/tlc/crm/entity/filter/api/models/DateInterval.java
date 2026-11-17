package com.tlc.crm.entity.filter.api.models;

import com.tlc.i18n.I18nKey;

/**
 * <p>
 * A model that represents the date interval data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DateInterval {

    private static final String BETWEEN = "BETWEEN";

    private Long id;
    private Long sourceId;
    private I18nKey displayName;
    private String name;
    private DateIntervalGroup group;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public I18nKey getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public DateIntervalGroup getGroup() {
        return group;
    }

    public void setGroup(final DateIntervalGroup group) {
        this.group = group;
    }

    public boolean isRange() {
        return BETWEEN.equals(name);
    }
}

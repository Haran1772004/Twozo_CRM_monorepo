package com.tlc.crm.core.segment.models;

/**
 * <p>
 * Represents the default segment filters.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SegmentDefaultFilter {

    ALL_SEGMENTS((short) 1, "i18n.segment.filter.all.segments"),
    ARCHIVED_SEGMENTS((short) 2, "i18n.segment.filter.archived.segments"),
    ALL_FOLDERS((short) 3, "i18n.segment.filter.all.folders"),
    ;

    private final short value;
    private final String i18nKey;

    /**
     * <p>
     * Constructs the default segment filter.
     * </p>
     *
     * @param value   Represents the filter id.
     * @param i18nKey Represents the i18n key.
     */
    SegmentDefaultFilter(final short value, final String i18nKey) {
        this.value = value;
        this.i18nKey = i18nKey;
    }

    /**
     * <p>
     * Gets the filter id.
     * </p>
     *
     * @return Filter id.
     */
    public Short getValue() {
        return value;
    }

    /**
     * <p>
     * Gets the i18n key.
     * </p>
     *
     * @return I18n key.
     */
    public String getI18nKey() {
        return i18nKey;
    }

    /**
     * <p>
     * Gets the {@link SegmentDefaultFilter} by value.
     * </p>
     *
     * @param value Represents the filter id.
     * @return {@link SegmentDefaultFilter}.
     */
    public static SegmentDefaultFilter get(final int value) {
        for (final SegmentDefaultFilter filter : values()) {
            if (filter.value == value) {
                return filter;
            }
        }

        return null;
    }

    public boolean isAllSegments() {
        return this == ALL_SEGMENTS;
    }

    public boolean isArchivedSegments() {
        return this == ARCHIVED_SEGMENTS;
    }

    public boolean isAllFolders() {
        return this == ALL_FOLDERS;
    }
}

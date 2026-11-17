package com.tlc.crm.core.segment.models;

/**
 * <p>
 * Represents the segment filter preference groups.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public enum SegmentFilterGroup {

    DEFAULT_FILTERS((short) 1, "i18n.segment.filter.group.default.filters"),
    STARRED_FOLDERS((short) 2, "i18n.segment.filter.group.starred.folders"),
    FOLDERS((short) 3, "i18n.segment.filter.group.folders"),
    ;

    private final short value;
    private final String i18nKey;

    /**
     * <p>
     * Constructs the segment filter group.
     * </p>
     *
     * @param value   Represents the group id.
     * @param i18nKey Represents the i18n key.
     */
    SegmentFilterGroup(final short value, final String i18nKey) {
        this.value = value;
        this.i18nKey = i18nKey;
    }

    /**
     * <p>
     * Gets the group id.
     * </p>
     *
     * @return Group id.
     */
    public int getValue() {
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
     * Gets the {@link SegmentFilterGroup} by value.
     * </p>
     *
     * @param value Represents the group id.
     * @return {@link SegmentFilterGroup}.
     */
    public static SegmentFilterGroup get(final short value) {
        for (final SegmentFilterGroup group : values()) {
            if (group.value == value) {
                return group;
            }
        }

        return null;
    }

    public boolean isDefaultFilterGroup() {
        return this == DEFAULT_FILTERS;
    }

    public boolean isStarredFolderGroup() {
        return this == STARRED_FOLDERS;
    }
}

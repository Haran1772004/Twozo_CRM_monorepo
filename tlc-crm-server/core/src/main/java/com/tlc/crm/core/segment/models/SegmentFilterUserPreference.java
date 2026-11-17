package com.tlc.crm.core.segment.models;

import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.fields.ListActionFields;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.core.segment.fields.SegmentFields;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.util.Objects;

/**
 * <p>
 * Represents the segment filter preference of a user.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentFilterUserPreference implements TlcModel {

    private static final int DEFAULT_START = NumericalConstant.ONE;
    private static final int DEFAULT_COUNT = NumericalConstant.TWENTY_FIVE;

    private Long id;
    private Long orgId;
    private Long userId;

    /**
     * <p>
     * Request scoped values. These values are populated from the request
     * and are not persisted in the database.
     * </p>
     */
    private String searchText;

    /**
     * <p>
     * Persisted user preference.
     * </p>
     */
    private JsonObject sortJson;
    private JsonObject limitJson;
    private SegmentFilterGroup segmentFilterGroup;
    private SegmentDefaultFilter segmentDefaultFilter;
    private SegmentFolder segmentFolder;
    private DateTime modifiedTime;

    /**
     * <p>
     * Gets the organization id.
     * </p>
     *
     * @return Organization id.
     */
    public Long getOrgId() {
        return orgId;
    }

    /**
     * <p>
     * Sets the organization id.
     * </p>
     *
     * @param orgId Organization id.
     */
    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    /**
     * <p>
     * Gets the user id.
     * </p>
     *
     * @return User id.
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * <p>
     * Sets the user id.
     * </p>
     *
     * @param userId User id.
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * <p>
     * Gets the search text.
     * </p>
     *
     * @return Search text.
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * <p>
     * Sets the search text.
     * </p>
     *
     * @param searchText Search text.
     */
    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

    /**
     * <p>
     * Gets the sort configuration.
     * </p>
     *
     * @return Sort configuration.
     */
    public JsonObject getSortJson() {
        return sortJson;
    }

    /**
     * <p>
     * Sets the sort configuration.
     * </p>
     *
     * @param sortJson Sort configuration.
     */
    public void setSortJson(JsonObject sortJson) {
        this.sortJson = sortJson;
    }

    /**
     * <p>
     * Gets the sort field name.
     * </p>
     *
     * @return Sort field name.
     */
    public String getSortFieldName() {
        if (Objects.nonNull(sortJson)) {
            return sortJson.getString(CommonFields.FIELD_NAME);
        }

        return null;
    }

    /**
     * <p>
     * Gets the sort type.
     * </p>
     *
     * @return Sort type.
     */
    public String getSortType() {
        if (Objects.nonNull(sortJson)) {
            return sortJson.getString(CommonFields.TYPE);
        }
        return null;
    }

    /**
     * <p>
     * Gets the limit configuration.
     * </p>
     *
     * @return Limit configuration.
     */
    public JsonObject getLimitJson() {
        return limitJson;
    }

    /**
     * <p>
     * Sets the limit configuration.
     * </p>
     *
     * @param limitJson Limit configuration.
     */
    public void setLimitJson(JsonObject limitJson) {
        this.limitJson = limitJson;
    }

    /**
     * <p>
     * Gets the start value for pagination.
     * </p>
     *
     * @return Start value.
     */
    public int getStart() {
        return Objects.isNull(limitJson) ? DEFAULT_START : limitJson.getInt(ListActionFields.START);
    }

    /**
     * <p>
     * Gets the count value for pagination.
     * </p>
     *
     * @return Count value.
     */
    public int getCount() {
        return Objects.isNull(limitJson) ? DEFAULT_COUNT : limitJson.getInt(ListActionFields.COUNT);
    }

    /**
     * <p>
     * Gets the selected filter group.
     * </p>
     *
     * @return {@link SegmentFilterGroup}.
     */
    public SegmentFilterGroup getSegmentFilterGroup() {
        return segmentFilterGroup;
    }

    /**
     * <p>
     * Sets the selected filter group.
     * </p>
     *
     * @param segmentFilterGroup {@link SegmentFilterGroup}.
     */
    public void setSegmentFilterGroup(final SegmentFilterGroup segmentFilterGroup) {
        this.segmentFilterGroup = segmentFilterGroup;
    }

    /**
     * <p>
     * Gets the selected default filter.
     * </p>
     *
     * @return {@link SegmentDefaultFilter}.
     */
    public SegmentDefaultFilter getSegmentDefaultFilter() {
        return segmentDefaultFilter;
    }

    /**
     * <p>
     * Sets the selected default filter.
     * </p>
     *
     * @param segmentDefaultFilter {@link SegmentDefaultFilter}.
     */
    public void setSegmentDefaultFilter(final SegmentDefaultFilter segmentDefaultFilter) {
        this.segmentDefaultFilter = segmentDefaultFilter;
    }

    /**
     * <p>
     * Gets the selected folder.
     * </p>
     *
     * @return {@link SegmentFolder}.
     */
    public SegmentFolder getSegmentFolder() {
        return segmentFolder;
    }

    /**
     * <p>
     * Sets the selected folder.
     * </p>
     *
     * @param segmentFolder {@link SegmentFolder}.
     */
    public void setSegmentFolder(final SegmentFolder segmentFolder) {
        this.segmentFolder = segmentFolder;
    }

    /**
     * <p>
     * Gets the modified time.
     * </p>
     *
     * @return Modified time.
     */
    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    /**
     * <p>
     * Sets the modified time.
     * </p>
     *
     * @param modifiedTime Modified time.
     */
    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Sets the preference id.
     * </p>
     *
     * @param id Preference id.
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Resolves the segment status from the selected filter preference.
     * </p>
     *
     * @return {@link Segment.Status}.
     */
    public Segment.Status getStatus() {
        final SegmentFilterGroup group = this.getSegmentFilterGroup();

        if (Objects.isNull(group)) {
            return null;
        }

        if (!group.isDefaultFilterGroup()) {
            return Segment.Status.ACTIVE;
        }
        final SegmentDefaultFilter defaultFilter = this.getSegmentDefaultFilter();

        if (Objects.isNull(defaultFilter) || defaultFilter.isAllFolders()) {
            return null;
        }

        return defaultFilter.isAllSegments() ? Segment.Status.ACTIVE : Segment.Status.ARCHIVED;
    }

    /**
     * <p>
     * Resolves whether the starred folders filter is selected.
     * </p>
     *
     * @return {@code Boolean.TRUE} if starred folders are selected,
     * {@code Boolean.FALSE} if folders are selected,
     * otherwise {@code null}.
     */
    public Boolean getIsStarred() {
        final SegmentFilterGroup group = this.getSegmentFilterGroup();

        if (Objects.isNull(group) || group.isDefaultFilterGroup()) {
            return null;
        }

        return group.isStarredFolderGroup();
    }
}


package com.tlc.crm.entity.timeline.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.icon.model.Icon;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.HashSet;

/**
 * Model which represents the Timeline
 *
 * @author Kavinila E
 * @version 1.0
 */
public class Timeline implements TlcModel {

    private TimelineType timelineType;
    private Long id;
    private long orgId;
    private Long ownerId;
    private DateTime auditedTime;
    private String action;
    private Category category;
    private FieldTimeline fieldTimeline;
    private Icon icon;
    private String iconType;
    private CrmUser salesOwner;
    private final EntityTimelines entityTimelines;
    private Collection<Long> userIds;
    private FieldAuditInfo fieldAuditInfo;

    public Timeline(){
        entityTimelines = new EntityTimelines();
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;

        addUserId(ownerId);
    }

    public DateTime getAuditedTime() {
        return auditedTime;
    }

    public void setAuditedTime(final DateTime auditedTime) {
        this.auditedTime = auditedTime;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public void setTimelineType(final TimelineType timelineType) {
        this.timelineType = timelineType;
    }

    public void setIcon(final Icon icon) {
        this.icon = icon;
    }

    public void setIconType(final String iconType) {
        this.iconType = iconType;
    }

    public void setFieldTimeline(final FieldTimeline fieldTimeline) {
        this.fieldTimeline = fieldTimeline;
    }

    public void setSalesOwner(final CrmUser salesOwner) {
        this.salesOwner = salesOwner;
    }

    public void setEntityTimeline(final EntityTimeline entityTimeline) {
        entityTimelines.set(entityTimeline);
    }


    public EntityTimelines getEntityTimeline() {
        return entityTimelines;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getIconType() {
        return iconType;
    }

    public Category getCategory() {
        return category;
    }

    public String getAction() {
        return action;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public TimelineType getTimelineType() {
        return timelineType;
    }

    public FieldTimeline getFieldTimeline() {
        return fieldTimeline;
    }

    public CrmUser getSalesOwner() {
        return salesOwner;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public Long orgId() {
        return this.orgId;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Represents different categories of {@link Timeline}.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum Category {

        CONTACT,
        DEAL,
        COMPANY,
        NOTES,
        ACTIVITY,
        FILE,
        UPDATES,
        EMAILS,
        CALLS,
        CHATS,
        INVOICE,
        ;

        /**
         * <p>
         * Gets the {@link Category} of timeline.
         * </p>
         *
         * @param category Represents the {@link Category}
         * @return {@link Category}
         */
        public static Category get(final String category) {
            return Category.valueOf(category.toUpperCase());
        }
    }

    /**
     * <p>
     * Checks whether the input given is matching with the {@link Category}.
     * </p>
     *
     * @param category     Represents the value of {@link Category}.
     * @param categoryName input value referring category name.
     * @return whether the input is matching.
     */
    public static boolean isCategoryEquals(final Category category, final String categoryName) {
        return category.name().equalsIgnoreCase(categoryName);
    }

    public void addUserId(final Long userId) {
        this.userIds = this.userIds == null ? new HashSet<>() : this.userIds;

        this.userIds.add(userId);
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    /**
     * Returns the field edit context attached to this timeline, or the default user context if none is set.
     */
    public FieldAuditInfo getFieldAuditInfo() {
        return fieldAuditInfo == null ? FieldAuditInfo.defaultContext() : fieldAuditInfo;
    }

    /**
     * Sets the field edit context for this timeline. Should be set during timeline creation to capture the edit source and user.
     *
     * @param fieldAuditInfo the context of the field edit, including source and user information
     */
    public void setFieldAuditInfo(final FieldAuditInfo fieldAuditInfo) {
        this.fieldAuditInfo = fieldAuditInfo;
    }
}
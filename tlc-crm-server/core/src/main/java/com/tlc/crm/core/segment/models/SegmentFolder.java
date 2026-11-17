package com.tlc.crm.core.segment.models;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <p>
 * A model that represents the segment folder data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SegmentFolder implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.segment.folder.invalid.id")
    private Long id;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;
    @Size(max = 100, groups = {Group.Create.class, Group.Update.class}, message = "i18n.segment.folder.name.length.exceeded")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.segment.folder.invalid.name")
    private String name;
    private boolean starred;
    private CrmUser createdBy;
    private CrmUser modifiedBy;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private Long segmentCount;

    public SegmentFolder() {
    }

    public SegmentFolder(final Long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(final boolean starred) {
        this.starred = starred;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public CrmUser getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(final CrmUser modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(final DateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Long getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(final Long segmentCount) {
        this.segmentCount = segmentCount;
    }

    @Override
    public Object identity() {
        return null;
    }
}

package com.tlc.crm.core.segment.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.entity.campaign.model.CampaignRecipient;
import com.tlc.crm.entity.internal.status.SegmentErrorCodes;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the segment data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class Segment extends CampaignRecipient implements Cloneable {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.segment.invalid.id")
    private Long id;
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;
    @Size(max = 100, groups = {Group.Create.class, Group.Update.class}, message = "i18n.segment.name.length.exceeded")
    @NotBlank(groups = {Group.Create.class, Group.Update.class}, message = "i18n.segment.invalid.name")
    private String name;
    private Status status = Status.ACTIVE;
    @Valid
    private SegmentFolder folder;
    @Valid
    private Collection<SegmentConditionGroup> segmentConditionGroups;
    private CrmUser createdBy;
    private CrmUser modifiedBy;
    private DateTime createdTime;
    private DateTime modifiedTime;
    private DateTime lastCalculatedAt;
    private ComputationState computationState = ComputationState.UNCOMPUTED;
    private transient String groupsJson;

    public Segment() {
        super(RecipientType.SEGMENT);
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public SegmentFolder getFolder() {
        return folder;
    }

    public void setFolder(final SegmentFolder folder) {
        this.folder = folder;
    }

    public Collection<SegmentConditionGroup> getSegmentConditionGroups() {
        return segmentConditionGroups;
    }

    public void setSegmentConditionGroups(final Collection<SegmentConditionGroup> segmentConditionGroups) {
        this.segmentConditionGroups = segmentConditionGroups;
    }

    public void addGroup(final SegmentConditionGroup segmentConditionGroup) {
        segmentConditionGroups = Objects.isNull(segmentConditionGroups) ? new ArrayList<>() : segmentConditionGroups;
        segmentConditionGroups.add(segmentConditionGroup);
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

    public DateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(final DateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public ComputationState getComputationState() {
        return computationState;
    }

    public void setComputationState(final ComputationState computationState) {
        this.computationState = computationState;
    }

    public String getGroupsJson() {
        return groupsJson;
    }

    public void setGroupsJson(final String groupsJson) {
        this.groupsJson = groupsJson;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * <p>
     * Represents the status of a {@link Segment}.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Status {

        ACTIVE((short) 1),
        ARCHIVED((short) 2),
        ;

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static Status get(final short id) {
            return switch (id) {
                case 1 -> ACTIVE;
                case 2 -> ARCHIVED;
                default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_SEGMENT_STATUS);
            };
        }
    }

    /**
     * <p>
     * Represents the computation state of a {@link Segment}.
     * </p>
     *
     * @author  Mohammed Asif J.
     * @version 1.0
     */
    public enum ComputationState {

        UNCOMPUTED((short) 1),
        COMPUTING((short) 2),
        COMPUTED((short) 3),
        FAILED((short) 4),
        ;

        private final short id;

        ComputationState(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        public static ComputationState get(final short id) {
            return switch (id) {
                case 1 -> UNCOMPUTED;
                case 2 -> COMPUTING;
                case 3 -> COMPUTED;
                case 4 -> FAILED;
                default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_SEGMENT_COMPUTATION_STATE);
            };
        }

        public boolean isComputed() {
            return this == COMPUTED;
        }
    }
}
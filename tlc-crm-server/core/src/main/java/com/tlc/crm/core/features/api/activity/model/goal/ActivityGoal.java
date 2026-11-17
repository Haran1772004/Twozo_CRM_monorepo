package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.crm.common.models.CrmModel;
import com.tlc.crm.core.features.validator.annotation.ActivityGoalValidator;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * <p>
 * A Model which represents the ActivityGoal details
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Getter
@Setter
@ActivityGoalValidator(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.validation.failed")
public class ActivityGoal implements TlcModel, CrmModel {

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.activity.goal.id.required")
    private Long id;

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.org.id.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.owner.id.required")
    private Long ownerId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.activityType.id.required")
    private ActivityType activityType;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.goalPeriod.id.required")
    private GoalPeriod goalPeriod;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.goalRange.id.required")
    private GoalRange goalRange;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.goalType.id.required")
    private GoalType goalType;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.startTime.required")
    private Long startTime;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.endTime.required")
    private Long endTime;

    @Valid
    private TeamGoal teamGoal;

    @Valid
    private Collection<UserGoal> userGoals;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.attainment.required")
    private Attainment attainment;
    private Collection<Long> userIds;
    private Long createdTime;
    private Long updatedTime;
    private boolean isEditable;
    private boolean isDeletable;

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(final boolean editable) {
        isEditable = editable;
    }

    public boolean isDeletable() {
        return isDeletable;
    }

    public void setDeletable(final boolean deletable) {
        isDeletable = deletable;
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

    @Override
    public Collection<Long> userIds() {
        return userIds;
    }
}
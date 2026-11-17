package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * <p>
 * A Model which represents the TeamGoal details
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 * */
@Getter
@Setter
public class TeamGoal implements TlcModel {

    @Getter(AccessLevel.NONE)
    private Long id;

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamGoal.org.id.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamGoal.team.id.required")
    private Long teamId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamGoal.splitEnabledKey.required")
    private boolean isSplitEnabled;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamGoal.teamUserGoal.required")
    @Valid
    private Collection<TeamUserGoal> teamUserGoals;
    private String teamName;
    private boolean isActive;

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
}
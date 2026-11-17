package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * Model which represents the TeamUserGoal
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 * */
@Getter
@Setter
public class TeamUserGoal implements TlcModel {

    @Getter(AccessLevel.NONE)
    private Long id;

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamUserGoal.org.id.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamUserGoal.quotaEnabledKey.required")
    private boolean isQuotaEnabled;

    @Valid
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamUserGoal.attainment.required")
    private Attainment attainment;

    @Valid
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.teamUser.required")
    private TeamUser teamUser;
    private boolean isActiveUser;
    private boolean isRemovedFromTeam;

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
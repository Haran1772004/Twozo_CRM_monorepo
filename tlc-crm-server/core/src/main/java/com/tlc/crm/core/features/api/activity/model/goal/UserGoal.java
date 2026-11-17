package com.tlc.crm.core.features.api.activity.model.goal;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * A Model which represents the UserGoal details
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 * */
@Getter
@Setter
public class UserGoal implements TlcModel {

    @Getter(AccessLevel.NONE)
    private Long id;

    @Getter(AccessLevel.NONE)
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.userGoal.org.id.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.activity.goal.userGoal.crmUser.required")
    private CrmUser crmUser;
    private boolean isActiveUser;

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
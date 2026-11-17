package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.TeamUser;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * A model that represents the team user quota details
 *
 * @author kishore J
 * @version 1.0
 */
public class TeamUserQuota  implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.team.user.quota.invalid.org.id")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.team.user.quota.invalid.target")
    private Double target;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.team.user.quota.invalid.user")
    private TeamUser teamUser;

    private boolean isEnabled;
    private boolean isExcludedFromTeam;
    private boolean isNotPartOfTeamGoal;
    private Long teamQuotaId;

    public Double getTarget() {
        return target;
    }

    public void setTarget(final Double target) {
        this.target = target;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(final boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public boolean isActiveUser() {
        final CrmUser crmUser = teamUser.getCrmUser();

        return Objects.nonNull(crmUser) && crmUser.getState().isActive();
    }

    public TeamUser getTeamUser() {
        return teamUser;
    }

    public void setTeamUser(final TeamUser teamUser) {
        this.teamUser = teamUser;
    }

    public double getTargetValue() {
        return NumberUtils.parseNumber(target).doubleValue();
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
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }

    public boolean isExcludedFromTeam() {
        return isExcludedFromTeam;
    }

    public void setExcludedFromTeam(final boolean isRemovedFromTeam) {
        this.isExcludedFromTeam = isRemovedFromTeam;
    }

    public Long getTeamQuotaId() {
        return teamQuotaId;
    }

    public void setTeamQuotaId(final Long teamQuotaId) {
        this.teamQuotaId = teamQuotaId;
    }

    /**
     * Indicates whether the user is not yet part of the team quota.
     *
     * @return true if the user is not part of the team quota
     */
    public boolean isNotPartOfTeamGoal() {
        return isNotPartOfTeamGoal;
    }

    public void setNotPartOfTeamGoal(final boolean isNotPartOfTeamGoal) {
        this.isNotPartOfTeamGoal = isNotPartOfTeamGoal;
    }
}
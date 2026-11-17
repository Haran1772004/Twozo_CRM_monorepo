package com.tlc.crm.core.features.api.quota.model;

import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A model that represents the team quota details
 *
 * @author kishore J
 * @version 1.0
 */
public class TeamQuota extends Quota {

    private Long teamQuotaId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.team.quota.invalid.team")
    private Team team;

    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.quota.team.quota.invalid.team.users")
    @Valid
    private Collection<TeamUserQuota> teamUserQuotas;

    private boolean isActive;

    public Team getTeam() {
        return team;
    }

    public void setTeam(final Team team) {
        this.team = team;
    }

    public void setTeamQuotaId(final Long teamQuotaId) {
        this.teamQuotaId = teamQuotaId;
    }

    public Collection<TeamUserQuota> getTeamUserQuotas() {
        return teamUserQuotas;
    }

    public void setTeamUserQuotas(final Collection<TeamUserQuota> teamUserQuotas) {
        this.teamUserQuotas = teamUserQuotas;
    }

    public Long getTeamQuotaId() {
        return teamQuotaId;
    }

    public double getTotalTargetValue() {
        return Objects.isNull(teamUserQuotas) ? BigDecimal.ZERO.doubleValue()
                : teamUserQuotas.stream().mapToDouble(TeamUserQuota::getTargetValue).sum();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean isActive) {
        this.isActive = isActive;
    }

    public void addTeamQuotaUsers(final TeamUserQuota teamUserQuota) {
        teamUserQuotas = Objects.isNull(teamUserQuotas) ? new ArrayList<>() : teamUserQuotas;

        teamUserQuotas.add(teamUserQuota);
    }
}

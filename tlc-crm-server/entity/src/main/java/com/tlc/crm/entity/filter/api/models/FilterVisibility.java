package com.tlc.crm.entity.filter.api.models;

import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A model that represents the filter visibility data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class FilterVisibility {

    private Long id;
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.common.org.invalid.id")
    private Long orgId;
    @Valid
    private Visibility visibility;
    @Valid
    private VisibilityPermission visibilityPermission;
    private Collection<CrmUser> users;
    private Collection<Team> teams;
    private Collection<Territory> territories;
    private boolean preventShare;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(final Visibility visibility) {
        this.visibility = visibility;
    }

    public Collection<CrmUser> getUsers() {
        return users;
    }

    public void setUsers(final Collection<CrmUser> users) {
        this.users = users;
    }

    public Collection<Team> getTeams() {
        return teams;
    }

    public void setTeams(final Collection<Team> teams) {
        this.teams = teams;
    }

    public void addTeam(final Team team) {
        this.teams = Objects.isNull(this.teams) ? new ArrayList<>() : this.teams;

        this.teams.add(team);
    }

    public void addTerritory(final Territory territory) {
        this.territories = Objects.isNull(this.territories) ? new ArrayList<>() : this.territories;

        this.territories.add(territory);
    }

    public Collection<Territory> getTerritories() {
        return territories;
    }

    public void setTerritories(final Collection<Territory> territories) {
        this.territories = territories;
    }

    public VisibilityPermission getVisibilityPermission() {
        return visibilityPermission;
    }

    public void setVisibilityPermission(final VisibilityPermission visibilityPermission) {
        this.visibilityPermission = visibilityPermission;
    }

    public boolean isPreventShare() {
        return preventShare;
    }

    public void setPreventShare(final boolean preventShare) {
        this.preventShare = preventShare;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (Objects.isNull(object) || getClass() != object.getClass()) {
            return false;
        }
        final FilterVisibility filterVisibility = (FilterVisibility) object;

        return preventShare == filterVisibility.preventShare && Objects.equals(id, filterVisibility.id)
                && Objects.equals(orgId, filterVisibility.orgId) && visibility == filterVisibility.visibility
                && visibilityPermission == filterVisibility.visibilityPermission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orgId, visibility, visibilityPermission, preventShare);
    }
}

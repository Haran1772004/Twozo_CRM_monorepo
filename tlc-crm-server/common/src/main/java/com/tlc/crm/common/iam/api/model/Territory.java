package com.tlc.crm.common.iam.api.model;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.crm.common.action.search.annotation.SearchField;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link Territory} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class Territory implements TlcModel {

    @SearchField(name = "id", readMethodName = "id")
    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.territory.id.required")
    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.territory.name.required")
    private String name;

    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.territory.orgId.required")
    private Long orgId;
    private String description;
    private Collection<Team> teams;
    private Collection<CrmUser> crmUsers;
    private DateTime createdTime;
    private DateTime updatedTime;
    private Long usersCount;

    @NotNull(groups = {Group.Create.class, Group.Delete.class}, message = "i18n.createdBy.required")
    @Valid
    private CrmUser createdBy;

    @NotNull(groups = {Group.Create.class, Group.Delete.class}, message = "i18n.updatedBy.required")
    @Valid
    private CrmUser updatedBy;

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void setTeams(final Collection<Team> teams) {
        this.teams = teams;
    }

    public void setUsersCount(final Long usersCount) {
        this.usersCount = usersCount;
    }

    public void setCrmUsers(final Collection<CrmUser> crmUsers) {
        this.crmUsers = crmUsers;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void addUser(final CrmUser crmUser) {
        this.crmUsers = this.crmUsers == null ? new ArrayList<>() : this.crmUsers;

        this.crmUsers.add(crmUser);
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Collection<Team> getTeams() {
        return teams;
    }

    public Collection<CrmUser> getCrmUsers() {
        return crmUsers;
    }

    public Long getUsersCount() {
        return usersCount;
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
}

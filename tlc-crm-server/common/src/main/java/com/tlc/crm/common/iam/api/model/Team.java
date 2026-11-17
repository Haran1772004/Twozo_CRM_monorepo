package com.tlc.crm.common.iam.api.model;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A model that represents the {@link Team} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@Notifiable(types = AppNotificationEventType.ADDED_TO_TEAM, modelName = CrmModelType.TEAM)
public class Team implements TlcModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.team.id.required")
    private Long id;

    @Size(max = 255, groups = {Group.Create.class, Group.Update.class}, message = "i18n.team.name.length.exceeded")
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.team.name.required")
    private String name;
    @NotNull(groups = {Group.Create.class, Group.Update.class, Group.Delete.class}, message = "i18n.team.orgId.required")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.team.user.required")
    @Valid
    private Collection<TeamUser> users;
    private DateTime createdTime;
    private DateTime updatedTime;
    private Long usersCount;
    private Long managersCount;
    private Collection<Long> userIds;

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

    public void setUsers(final Collection<TeamUser> users) {
        this.users = users;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUsersCount(final Long usersCount) {
        this.usersCount = usersCount;
    }

    public void setCreatedBy(final CrmUser createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final CrmUser updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void addUser(final TeamUser crmUser) {
        this.users = this.users == null ? new ArrayList<>() : this.users;

        this.users.add(crmUser);
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public String getName() {
        return name;
    }

    public CrmUser getCreatedBy() {
        return createdBy;
    }

    public CrmUser getUpdatedBy() {
        return updatedBy;
    }

    public Collection<TeamUser> getUsers() {
        return users;
    }

    public Long getUsersCount() {
        return usersCount;
    }

    public Long getManagersCount() {
        return managersCount;
    }

    public void setManagersCount(final Long managersCount) {
        this.managersCount = managersCount;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    @Override
    public Object identity() {
        return null;
    }
}

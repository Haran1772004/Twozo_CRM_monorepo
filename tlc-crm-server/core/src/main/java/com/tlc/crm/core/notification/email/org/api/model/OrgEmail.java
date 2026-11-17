package com.tlc.crm.core.notification.email.org.api.model;

import com.tlc.crm.common.iam.api.model.CrmRole;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A model that represents an org email
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class OrgEmail implements EmailNotificationModel {

    @NotNull(groups = {Group.Update.class, Group.Delete.class}, message = "i18n.notifications.orgEmail.invalid.id")
    private Long id;

    @NotNull(groups = Group.Create.class, message = "i18n.notifications.orgEmail.invalid.orgId")
    private Long orgId;

    @NotNull(groups = Group.Create.class, message = "i18n.notifications.orgEmail.invalid.userId")
    private Long userId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notifications.orgEmail.invalid.name")
    private String name;

    @NotNull(groups = Group.Create.class, message = "i18n.notifications.orgEmail.invalid.emailAddress")
    private EmailAddress address;
    private boolean isVerified;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.notifications.orgEmail.invalid.roles")
    private Collection<CrmRole> crmRoles;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(final boolean verified) {
        isVerified = verified;
    }

    public EmailAddress getAddress() {
        return address;
    }

    public void setAddress(final EmailAddress address) {
        this.address = address;
    }

    @Override
    public Object identity() {
        return null;
    }

    public Collection<CrmRole> getCrmRoles() {
        return crmRoles;
    }

    public void setCrmRoles(final Collection<CrmRole> roles) {
        this.crmRoles = roles;
    }

    public void addCrmRole(final CrmRole crmRole) {
        this.crmRoles = this.crmRoles == null ? new ArrayList<>() : this.crmRoles;

        this.crmRoles.add(crmRole);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }
}

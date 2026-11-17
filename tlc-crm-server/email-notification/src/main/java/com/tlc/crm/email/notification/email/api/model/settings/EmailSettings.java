package com.tlc.crm.email.notification.email.api.model.settings;

import com.tlc.crm.email.notification.email.EmailNotificationModel;
import com.tlc.crm.email.notification.email.validator.Group;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * A model to manage the settings of an email.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class EmailSettings implements EmailNotificationModel, Serializable {

    private Long id;

    @NotNull(groups = {Group.EmailSettings.class}, message = "i18n.common.org.invalid.id")
    private long orgId;

    @NotNull(groups = {Group.EmailSettings.class}, message = "i18.common.user.invalid.id")
    private long userId;

    @Valid
    private Collection<UserBlockedEmailAddress> userBlockedEmailAddresses;

    private EmailSignature emailSignature;
    private EmailTrackerSettings emailTrackerSettings;
    private EmailLinkerSettings emailLinkerSettings;

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

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setEmailTrackerSettings(final EmailTrackerSettings emailTrackerSettings) {
        this.emailTrackerSettings = emailTrackerSettings;
    }

    public EmailTrackerSettings getEmailTrackerSettings() {
        return emailTrackerSettings;
    }

    public void setEmailLinkerSettings(final EmailLinkerSettings emailLinkerSettings) {
        this.emailLinkerSettings = emailLinkerSettings;
    }

    public EmailLinkerSettings getEmailLinkerSettings() {
        return emailLinkerSettings;
    }

    public Collection<UserBlockedEmailAddress> getUserBlockedEmailAddresses() {
        return userBlockedEmailAddresses;
    }

    public void setUserBlockedEmailAddresses(final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses) {
        this.userBlockedEmailAddresses = userBlockedEmailAddresses;
    }

    public EmailSignature getEmailSignature() {
        return emailSignature;
    }

    public void setEmailSignature(final EmailSignature emailSignature) {
        this.emailSignature = emailSignature;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    @Override
    public Object identity() {
        return null;
    }
}

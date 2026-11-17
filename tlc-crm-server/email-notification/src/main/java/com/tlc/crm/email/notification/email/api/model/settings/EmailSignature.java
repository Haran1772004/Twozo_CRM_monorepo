package com.tlc.crm.email.notification.email.api.model.settings;


import com.tlc.crm.email.notification.email.validator.Group;
import com.tlc.validator.TlcModel;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * <p>
 * A model that represents an email signature
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSignature implements TlcModel, Serializable {

    private Long id;

    @NotNull(groups = {Group.EmailSignature.class}, message = "i18n.common.org.invalid.id")
    private long orgId;

    @NotNull(groups = {Group.EmailSignature.class}, message = "i18.common.user.invalid.id")
    private long userId;

    @NotNull(groups = {Group.EmailSignature.class}, message = "i18n.notification.mail.signature.invalid")
    @Size(max = 10000, groups = {Group.EmailSignature.class}, message = "i18n.notification.mail.signature.length.exceeded")
    private String emailSignature;

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

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public String getEmailSignature() {
        return emailSignature;
    }

    public void setEmailSignature(final String emailSignature) {
        this.emailSignature = emailSignature;
    }

    @Override
    public Object identity() {
        return null;
    }
}

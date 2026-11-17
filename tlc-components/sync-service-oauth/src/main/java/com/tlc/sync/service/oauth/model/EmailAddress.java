package com.tlc.sync.service.oauth.model;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * A model that represents the email address.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailAddress implements TlcModel {

    private Long id;
    private long orgId;
    private final String emailId;

    public EmailAddress(final String emailId) {
        this.emailId = emailId;
    }

    public EmailAddress(final Long orgId, final String emailId) {
        this.orgId = orgId;
        this.emailId = emailId;
    }

    public EmailAddress(final Long id, final Long orgId, final String emailId) {
        this.id = id;
        this.orgId = orgId;
        this.emailId = emailId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getEmailId() {
        return emailId;
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

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }
}

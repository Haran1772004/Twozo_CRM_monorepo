package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents the filter preferences for email domains for a specific user within an organization.
 * </p>
 *
 * @author Mohamed YasarK
 * @version 1.0
 */
public class EmailDomainFilterPreference implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private short statusFilter;
    private Long createdTime;
    private Long updatedTime;

    public EmailDomainFilterPreference() {
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public short getStatus() {
        return statusFilter;
    }

    public void setStatusFilter(final short statusFilter) {
        this.statusFilter = statusFilter;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Long updatedTime) {
        this.updatedTime = updatedTime;
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
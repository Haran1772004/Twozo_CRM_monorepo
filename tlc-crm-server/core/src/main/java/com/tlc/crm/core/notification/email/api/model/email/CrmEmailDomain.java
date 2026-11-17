package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.commons.dns.model.Domain;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * <p>
 * Represents a CRM-specific email domain entity.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class CrmEmailDomain extends Domain implements EmailNotifiableModel, TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long createdAt;
    private boolean isActive;
    private DomainStatus domainStatus;
    private Long emailDomainId;

    public void setEmailDomainId(final Long emailDomainId) {
        this.emailDomainId = emailDomainId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getEmailDomainId() {
        return emailDomainId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setDomainStatus(final DomainStatus domainStatus) {
        this.domainStatus = domainStatus;
    }

    public DomainStatus getDomainStatus() {
        return domainStatus;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final Long createdAt) {
        this.createdAt = createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public Collection<CrmEmailDnsRecord> getCrmDnsRecords() {
        return getDnsRecords().stream()
                .filter(record -> record instanceof CrmEmailDnsRecord)
                .map(record -> (CrmEmailDnsRecord) record)
                .collect(Collectors.toSet());
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(final boolean active) {
        isActive = active;
    }
}
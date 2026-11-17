package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.commons.dns.model.DnsRecord;
import com.tlc.email.common.model.Category;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents a CRM-specific DNS record entity.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class CrmEmailDnsRecord extends DnsRecord implements EmailNotifiableModel, TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long domainId;
    private Long emailDnsRecordId;
    private Category category;

    public Long getEmailDnsRecordId() {
        return emailDnsRecordId;
    }

    public void setEmailDnsRecordId(final Long emailDnsRecordId) {
        this.emailDnsRecordId = emailDnsRecordId;
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

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(final Long domainId) {
        this.domainId = domainId;
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

    public void setId(final Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }
}
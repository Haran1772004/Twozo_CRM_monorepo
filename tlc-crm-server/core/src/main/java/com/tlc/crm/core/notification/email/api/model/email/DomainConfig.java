package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents configuration settings associated with a CRM email domain.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class DomainConfig implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Long domainId;
    private String defaultSenderName;
    private boolean includeDefaultFromAsInReplyTo;
    private FromAddressInfo defaultFromAddress;
    private EmailAddress defaultReplyToAddress;

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

    public void setOrgId(Long orgId) {
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

    public boolean isIncludeDefaultFromAsInReplyTo() {
        return includeDefaultFromAsInReplyTo;
    }

    public void setIncludeDefaultFromAsInReplyTo(final boolean includeDefaultFromAsInReplyTo) {
        this.includeDefaultFromAsInReplyTo = includeDefaultFromAsInReplyTo;
    }

    public String getDefaultSenderName() {
        return defaultSenderName;
    }

    public void setDefaultSenderName(final String defaultSenderName) {
        this.defaultSenderName = defaultSenderName;
    }

    public FromAddressInfo getDefaultFromAddress() {
        return defaultFromAddress;
    }

    public void setDefaultFromAddress(final FromAddressInfo defaultFromAddress) {
        this.defaultFromAddress = defaultFromAddress;
    }

    public EmailAddress getDefaultReplyToAddress() {
        return defaultReplyToAddress;
    }

    public void setDefaultReplyToAddress(final EmailAddress defaultReplyToAddress) {
        this.defaultReplyToAddress = defaultReplyToAddress;
    }
}

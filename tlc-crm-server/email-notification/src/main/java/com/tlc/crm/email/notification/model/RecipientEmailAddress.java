package com.tlc.crm.email.notification.model;

import com.tlc.email.common.model.address.EmailAddress;

/**
 * Represents an email address class that is used to refer TO, CC and BCC
 *
 * @author Maharaja S
 * @version 1.0
 */
public class RecipientEmailAddress extends EmailAddress {

    private Long orgId;
    private Long userId;
    private Long crmEmailId;

    public RecipientEmailAddress(final String emailAddress) {
        super(emailAddress);
    }

    public RecipientEmailAddress(final Long crmEmailId, final String emailAddress) {
        super(emailAddress);
        this.crmEmailId = crmEmailId;
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

    public Long getCrmEmailId() {
        return crmEmailId;
    }

    public void setCrmEmailId(final Long crmEmailId) {
        this.crmEmailId = crmEmailId;
    }
}

package com.tlc.crm.email.notification.model;

import com.tlc.email.common.model.address.SyncAddressInfo;

/**
 * Represents a sync email address class that is used to set sender
 *
 * @author Maharaja S
 * @version 1.0
 */
public class UserSyncAddressInfo extends SyncAddressInfo {

    private Long orgId;
    private Long userId;
    private Long crmEmailId;

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

    public UserSyncAddressInfo(final String emailAddress) {
        super(emailAddress);
    }
}

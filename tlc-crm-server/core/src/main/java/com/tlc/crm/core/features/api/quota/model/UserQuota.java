package com.tlc.crm.core.features.api.quota.model;

import com.tlc.crm.common.iam.api.model.CrmUser;

import java.util.Collection;

/**
 * A model that represents the user quota details
 *
 * @author kishore J
 * @version 1.0
 */
public class UserQuota extends Quota {

    private Long userQuotaId;

    private Collection<CrmUser> crmUsers;

    public Collection<CrmUser> getCrmUsers() {
        return crmUsers;
    }

    public void setCrmUsers(final Collection<CrmUser> crmUsers) {
        this.crmUsers = crmUsers;
    }

    public void setUserQuotaId(final Long userQuotaId) {
        this.userQuotaId = userQuotaId;
    }

    public Long getUserQuotaId() {
        return userQuotaId;
    }

    public boolean isActiveUser() {
        return !crmUsers.isEmpty() && crmUsers.iterator().next().getState().isActive();
    }
}

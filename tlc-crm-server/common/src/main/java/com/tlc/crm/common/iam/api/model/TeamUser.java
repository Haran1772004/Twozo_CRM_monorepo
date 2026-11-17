package com.tlc.crm.common.iam.api.model;

import java.io.Serializable;

/**
 * A model which represents the {@link TeamUser} details.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TeamUser implements Serializable {

    private CrmUser crmUser;

    private boolean isManager;

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(final boolean manager) {
        this.isManager = manager;
    }
}

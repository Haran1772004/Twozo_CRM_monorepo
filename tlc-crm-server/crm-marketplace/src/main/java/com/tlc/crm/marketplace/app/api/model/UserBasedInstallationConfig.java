package com.tlc.crm.marketplace.app.api.model;

import com.tlc.marketplace.model.InstallationConfig;

/**
 * Refers the model of {@code OrgBasedInstallationConfig} that extends {@code InstallationConfiguration}.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class UserBasedInstallationConfig extends InstallationConfig {

    private Long orgId;
    private Long userId;

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }
}
package com.tlc.crm.marketplace.app.api.model;

import com.tlc.marketplace.model.InstallationConfig;

/**
 * Refers the model of {@code OrgBasedInstallationConfig} that extends {@code InstallationConfiguration}.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public class OrgBasedInstallationConfig extends InstallationConfig {

    private Long orgId;

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }
}
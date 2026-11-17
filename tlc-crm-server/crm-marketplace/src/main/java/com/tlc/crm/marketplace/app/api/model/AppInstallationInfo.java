package com.tlc.crm.marketplace.app.api.model;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.marketplace.model.AppState;
import com.tlc.marketplace.model.InstallationConfig;
import com.tlc.validator.TlcModel;

/**
 * Refers the model of {@code AppInstallationInfo}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AppInstallationInfo implements TlcModel {

    private Long id;
    private Long orgId;
    private CrmUser crmUser;
    private Long userId;
    private Long appId;
    private CrmMarketplaceApp crmMarketPlaceApp;
    private AppState appState;
    private Long installedTime;
    private ApplicationScope applicationScope;
    private InstallationConfig installationConfig;

    public AppInstallationInfo() {
    }

    public AppInstallationInfo(final Long appId, final Long orgId, final Long userId, final ApplicationScope applicationScope) {
        this.appId = appId;
        this.orgId = orgId;
        this.userId = userId;
        this.applicationScope = applicationScope;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public CrmUser getCrmUser() {
        return crmUser;
    }

    public void setCrmUser(final CrmUser crmUser) {
        this.crmUser = crmUser;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public Long getAppId() {
        return appId;
    }

    public void setAppId(final Long appId) {
        this.appId = appId;
    }

    public CrmMarketplaceApp getMarketPlaceApp() {
        return crmMarketPlaceApp;
    }

    public void setMarketPlaceApp(final CrmMarketplaceApp crmMarketPlaceApp) {
        this.crmMarketPlaceApp = crmMarketPlaceApp;
    }

    public Long getInstalledTime() {
        return installedTime;
    }

    public void setInstalledTime(final Long installedTime) {
        this.installedTime = installedTime;
    }

    public AppState getAppState() {
        return appState;
    }

    public void setAppState(final AppState appState) {
        this.appState = appState;
    }

    public InstallationConfig getInstallationConfig() {
        return installationConfig;
    }

    public void setInstallationConfig(final InstallationConfig installationConfig) {
        this.installationConfig = installationConfig;
    }

    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    public void setApplicationScope(final ApplicationScope applicationScope) {
        this.applicationScope = applicationScope;
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

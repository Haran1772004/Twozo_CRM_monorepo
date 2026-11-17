package com.tlc.crm.zapier.marketplace.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.marketplace.app.api.model.ApplicationScope;
import com.tlc.crm.marketplace.app.api.model.AppInstallationInfo;
import com.tlc.crm.marketplace.app.api.model.InstallationType;
import com.tlc.crm.marketplace.app.handler.MarketplaceAppExecutionHandler;
import com.tlc.crm.marketplace.app.handler.MarketplaceAppHandler;

import com.tlc.crm.zapier.api.marketplace.action.manager.ZapierAppManager;
import com.tlc.crm.zapier.fields.ZapAttributes;
import com.tlc.crm.zapier.internal.status.ZapierErrorCodes;

import com.tlc.marketplace.model.AppState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Implements the {@link MarketplaceAppExecutionHandler} and Handles the zapier related operations
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@MarketplaceAppHandler(app = "zapier")
public final class ZapierExecutionHandler extends MarketplaceAppExecutionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZapierExecutionHandler.class);

    private final ZapierAppManager zapierAppManager;
    private final Configurations configurations;

    public ZapierExecutionHandler() {
        this.zapierAppManager = ZapierAppManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties("zapier");
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization where the app is to be installed.
     * @param appId    The ID of the application to be installed.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    @Override
    protected void install(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.INSTALLED);
        appInstallationInfo.setAppId(appId);
        appInstallationInfo.setApplicationScope(ApplicationScope.USER_BASED);

        try {
            zapierAppManager.install(appInstallationInfo);
            response.put(ZapAttributes.TYPE, InstallationType.VENDOR_DEPENDENT.getName());
            response.put(ZapAttributes.REDIRECT_URL, configurations.getConfig("redirect.url"));
        } catch (Exception exception) {
            LOGGER.error("Marketplace: Zapier app installation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

            response.setErrorCode(ErrorCode.get(ZapierErrorCodes.INSTALLATION_FAILED));
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.installation.failed"));
        } finally {
            response.send();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The ID of the organization where the app is to be uninstalled.
     * @param appId    The ID of the application to be uninstalled.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    @Override
    protected void uninstall(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final AppInstallationInfo appInstallationInfo = new AppInstallationInfo();

        appInstallationInfo.setUserId(request.userId());
        appInstallationInfo.setOrgId(orgId);
        appInstallationInfo.setAppState(AppState.UNINSTALLED);
        appInstallationInfo.setAppId(appId);
        appInstallationInfo.setApplicationScope(ApplicationScope.USER_BASED);

        if (zapierAppManager.hasUserInstalledApp(appInstallationInfo)) {
            try {
                zapierAppManager.uninstall(appInstallationInfo);
                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.uninstallation.success"));
            } catch (Exception exception) {
                LOGGER.error("Marketplace: Zapier app uninstallation failed. Org ID: {}, Reason: {}", orgId, exception.getMessage());

                response.setErrorCode(ErrorCode.get(ZapierErrorCodes.UNINSTALLATION_FAILED));
                response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.uninstallation.failed"));
            } finally {
                response.send();
            }
        } else {
            response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.app.not.exists"));
            response.send();
        }
    }

    /**
     * Handles the marketplace app operations.
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    @Override
    protected void executeAction(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        throw ErrorCode.get(ZapierErrorCodes.UNSUPPORTED_ACTION, "i18n.crm.zapier.unsupported.action");
    }
}